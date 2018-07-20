/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.indexer;

import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import dk.dbc.rawrepo.exception.SolrIndexerRawRepoException;
import dk.dbc.rawrepo.exception.SolrIndexerSolrException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.queue.RawRepoQueueDAO;
import dk.dbc.util.Stopwatch;
import dk.dbc.util.Timed;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.MDC;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * @author DBC {@literal <dk.dbc.dk>}
 */
@Stateless
public class Indexer {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(Indexer.class);

    private  static final String TRACKING_ID = "trackingId";

    @Inject
    @ConfigProperty(name = "SOLR_URL", defaultValue = "SOLR_URL not set")
    protected String SOLR_URL;

    @Inject
    @ConfigProperty(name = "WORKER", defaultValue = "WORKER not set")
    protected String WORKER;

    @Inject
    @ConfigProperty(name = "OPENAGENCY_URL", defaultValue = "OPENAGENCY_URL not set")
    protected String openAgencyUrl;

    @Resource(lookup = "jdbc/rawrepo")
    protected DataSource rawrepoDataSource;

    @EJB
    private RawRepoRecordBean recordBean;

    static final String MIMETYPE_MARCXCHANGE = "text/marcxchange";
    static final String MIMETYPE_ENRICHMENT = "text/enrichment+marcxchange";
    static final String MIMETYPE_ARTICLE = "text/article+marcxchange";
    static final String MIMETYPE_AUTHORITY = "text/authority+marcxchange";

    JavaScriptWorker worker;

    private SolrServer solrServer;

    public Indexer() {
        this.solrServer = null;
    }

    @PostConstruct
    public void create() {
        LOGGER.info("Initializing with url {}", SOLR_URL);
        solrServer = new HttpSolrServer(SOLR_URL);
        worker = new JavaScriptWorker();
    }

    @PreDestroy
    public void destroy() {
        solrServer.shutdown();
    }

    public void performWork() throws SolrIndexerRawRepoException, SolrIndexerSolrException {

        boolean moreWork = true;
        int processedJobs = 0;

        // Test connection and return proper error if there is a problem
        try {
            solrServer.ping();
        } catch (SolrServerException | IOException | HttpSolrServer.RemoteSolrException ex) {
            throw new SolrIndexerSolrException("Could not connect to the solr server. " + ex.getMessage(), ex);
        }

        final Stopwatch dequeueStopwatch = new Stopwatch();

        while (moreWork) {
            try (Connection connection = getConnection()) {
                final RawRepoQueueDAO dao = createDAO(connection);
                try {
                    dequeueStopwatch.reset();
                    final QueueItem job = dequeueJob(dao);
                    long dequeueDurationInMS = dequeueStopwatch.getElapsedTime(TimeUnit.MILLISECONDS);

                    if (job != null) {
                        LOGGER.info("---------------------------------------------------------------");
                        LOGGER.info("Dequeued job in {} ms", dequeueDurationInMS);
                        MDC.put(TRACKING_ID, createTrackingId(job));
                        processJob(job, dao);
                        commit(connection);
                        processedJobs++;
                        if (processedJobs % 1000 == 0) {
                            LOGGER.info("Still indexing {} jobs from '{}'", processedJobs, WORKER);
                        }
                    } else {
                        moreWork = false;
                    }
                } catch (QueueException | IllegalArgumentException | SQLException ex) {
                    connection.rollback();
                    throw ex;
                }
            } catch (SQLException ex) {
                // If we get a SQLException there is something wrong which we can't do anything about.
                LOGGER.error("SQLException: ", ex);
                throw new SolrIndexerRawRepoException("SQL exception from rawrepo:" + ex.toString(), ex);
            } catch (QueueException | RuntimeException ex) {
                LOGGER.error("Error getting job from database", ex);
                moreWork = false;
            } finally {
                MDC.remove(TRACKING_ID);
            }
        }
        if (processedJobs > 0) {
            LOGGER.info("Done indexing {} jobs from '{}'", processedJobs, WORKER);
        }
    }

    protected Connection getConnection() throws SQLException {
        final Connection connection = rawrepoDataSource.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    private RawRepoQueueDAO createDAO(final Connection connection) throws QueueException {
        return RawRepoQueueDAO.builder(connection).build();
    }

    @Timed
    public void processJob(QueueItem item, RawRepoQueueDAO dao) throws QueueException, SolrIndexerSolrException {
        LOGGER.info("Indexing {}", item);
        final String bibliographicRecordId = item.getBibliographicRecordId();
        final int agencyId = item.getAgencyId();

        final RecordIdDTO jobId = new RecordIdDTO();
        jobId.setBibliographicRecordId(bibliographicRecordId);
        jobId.setAgencyId(agencyId);

        try {
            final RecordDTO record = fetchRecord(bibliographicRecordId, agencyId);
            if (record == null) {
                LOGGER.info("record from {} does not exist, most likely queued by dependency", item);
                return;
            }
            MDC.put(TRACKING_ID, createTrackingId(item, record));
            if (record.isDeleted()) {
                deleteSolrDocument(jobId);
            } else {
                SolrInputDocument doc = createIndexDocument(record);
                updateSolr(jobId, doc);
            }
            LOGGER.info("Indexed {}", item);
        } catch (QueueException ex) {
            LOGGER.error("Queued record does not exist {}", item);
            queueFail(dao, item, ex.getMessage());
        } catch (HttpSolrServer.RemoteSolrException ex) {
            // Index is missing on the solr server so we need to stop now
            if (ex.getMessage().contains("unknown field")) {
                throw new SolrIndexerSolrException("Missing index: " + ex.getMessage(), ex);
            }
            LOGGER.error("Error processing {}", item, ex);
            queueFail(dao, item, ex.getMessage());
        } catch (SolrException | SolrServerException | IOException ex) {
            LOGGER.error("Error processing {}", item, ex);
            queueFail(dao, item, ex.getMessage());
        }
    }

    private QueueItem dequeueJob(final RawRepoQueueDAO dao) throws QueueException {
        return dao.dequeue(WORKER);
    }

    private RecordDTO fetchRecord(String bibliographicRecordId, int agencyId) throws QueueException {
        if (!recordBean.recordExistsMaybeDeleted(bibliographicRecordId, agencyId)) {
            return null;
        } else {
            return recordBean.fetchRecord(bibliographicRecordId, agencyId);
        }
    }

    private String createSolrDocumentId(RecordIdDTO recordId) {
        LOGGER.entry();
        String result = null;
        try {
            result = recordId.getBibliographicRecordId() + ":" + recordId.getAgencyId();

            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    SolrInputDocument createIndexDocument(RecordDTO record) {
        final SolrInputDocument doc = new SolrInputDocument();
        RecordIdDTO recordId = record.getRecordId();
        doc.addField("id", createSolrDocumentId(recordId));

        String mimeType = record.getMimetype();
        switch (mimeType) {
            case MIMETYPE_MARCXCHANGE:
            case MIMETYPE_ARTICLE:
            case MIMETYPE_AUTHORITY:
            case MIMETYPE_ENRICHMENT:
                LOGGER.debug("Indexing content of {} with mimetype {}", recordId, mimeType);
                String content = new String(record.getContent(), StandardCharsets.UTF_8);
                try {
                    worker.addFields(doc, content, mimeType);
                } catch (Exception ex) {
                    LOGGER.error("Error adding fields for document '{}': ", content, ex);
                }
                break;
            default:
                LOGGER.debug("Skipping indexing of {} with mimetype {}", recordId, mimeType);
        }

        doc.addField("rec.bibliographicRecordId", recordId.getBibliographicRecordId());
        doc.addField("rec.agencyId", recordId.getAgencyId());
        doc.addField("rec.created", record.getCreated());
        doc.addField("rec.modified", record.getModified());
        doc.addField("rec.trackingId", record.getTrackingId());
        LOGGER.trace("Created solr document {}", doc);
        return doc;
    }

    private void deleteSolrDocument(RecordIdDTO jobId) throws IOException, SolrServerException {
        LOGGER.debug("Deleting document for {} to solr", jobId);
        solrServer.deleteById(createSolrDocumentId(jobId));
    }

    private void updateSolr(RecordIdDTO jobId, SolrInputDocument doc) throws IOException, SolrServerException {
        LOGGER.debug("Adding document for {} to solr", jobId);
        solrServer.add(doc);
    }

    private void queueFail(RawRepoQueueDAO dao, QueueItem job, String error) throws QueueException {
        dao.queueFail(job, error);
    }

    private void commit(final Connection connection) throws SQLException {
        connection.commit();
    }

    private static String createTrackingId(QueueItem job) {
        return "RawRepoIndexer:" + job.toString();
    }

    private static String createTrackingId(QueueItem job, RecordDTO record) {
        String trackingId = record.getTrackingId();
        if (trackingId == null || trackingId.isEmpty()) {
            return createTrackingId(job);
        } else {
            return createTrackingId(job) + "<" + trackingId;
        }
    }
}
