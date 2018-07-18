/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.indexer;

import dk.dbc.marcxmerge.MarcXChangeMimeType;
import dk.dbc.marcxmerge.MarcXMerger;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.openagency.client.OpenAgencyServiceFromURL;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoDAO;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.RawRepoExceptionRecordNotFound;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RelationHintsOpenAgency;
import dk.dbc.rawrepo.exception.SolrIndexerRawRepoException;
import dk.dbc.rawrepo.exception.SolrIndexerSolrException;
import dk.dbc.util.Stopwatch;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
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

    private final static Logger log = LoggerFactory.getLogger(Indexer.class);

    private final static String TRACKING_ID = "trackingId";

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


    @Inject
    MergerPool mergerPool;

    JavaScriptWorker worker;

    private SolrServer solrServer;

    private OpenAgencyServiceFromURL openAgency;

    public Indexer() {
        this.solrServer = null;
    }

    @PostConstruct
    public void create() {
        // Read solr url from application context
        log.info("Initializing with url {}", SOLR_URL);
        solrServer = new HttpSolrServer(SOLR_URL);
        openAgency = OpenAgencyServiceFromURL.builder().build(openAgencyUrl);

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
                RawRepoDAO dao = createDAO(connection);
                try {
                    dequeueStopwatch.reset();
                    QueueJob job = dequeueJob(dao);
                    long dequeueDurationInMS = dequeueStopwatch.getElapsedTime(TimeUnit.MILLISECONDS);

                    if (job != null) {
                        log.info("---------------------------------------------------------------");
                        log.info("Dequeued job in {} ms", dequeueDurationInMS);
                        MDC.put(TRACKING_ID, createTrackingId(job));
                        processJob(job, dao);
                        commit(connection);
                        processedJobs++;
                        if (processedJobs % 1000 == 0) {
                            log.info("Still indexing {} jobs from '{}'", processedJobs, WORKER);
                        }
                    } else {
                        moreWork = false;
                    }
                } catch (RawRepoException | IllegalArgumentException | SQLException ex) {
                    connection.rollback();
                    throw ex;
                }
            } catch (SQLException ex) {
                // If we get a SQLException there is something wrong which we can't do anything about.
                log.error("SQLException: ", ex);
                throw new SolrIndexerRawRepoException("SQL exception from rawrepo:" + ex.toString(), ex);
            } catch (RawRepoException | RuntimeException ex) {
                log.error("Error getting job from database", ex);
                moreWork = false;
            } finally {
                MDC.remove(TRACKING_ID);
            }
        }
        if (processedJobs > 0) {
            log.info("Done indexing {} jobs from '{}'", processedJobs, WORKER);
        }
    }

    protected Connection getConnection() throws SQLException {
        final Connection connection = rawrepoDataSource.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    private RawRepoDAO createDAO(final Connection connection) throws RawRepoException {
        return RawRepoDAO.builder(connection).relationHints(new RelationHintsOpenAgency(openAgency)).build();
    }

    private void processJob(QueueJob job, RawRepoDAO dao) throws RawRepoException, SolrIndexerSolrException {
        log.info("Indexing {}", job);
        RecordId jobId = job.getJob();
        String id = jobId.getBibliographicRecordId();
        int library = jobId.getAgencyId();
        try {
            Record record = fetchRecord(dao, id, library);
            if (record == null) {
                log.info("record from {} does not exist, most likely queued by dependency", job);
                return;
            }
            MDC.put(TRACKING_ID, createTrackingId(job, record));
            if (record.isDeleted()) {
                deleteSolrDocument(jobId);
            } else {
                SolrInputDocument doc = createIndexDocument(record);
                updateSolr(jobId, doc);
            }
            log.info("Indexed {}", job);
        } catch (RawRepoExceptionRecordNotFound ex) {
            log.error("Queued record does not exist {}", job);
            queueFail(dao, job, ex.getMessage());
        } catch (HttpSolrServer.RemoteSolrException ex) {
            // Index is missing on the solr server so we need to stop now
            if (ex.getMessage().contains("unknown field")) {
                throw new SolrIndexerSolrException("Missing index: " + ex.getMessage(), ex);
            }
            log.error("Error processing {}", job, ex);
            queueFail(dao, job, ex.getMessage());
        } catch (RawRepoException | MarcXMergerException | SolrException | SolrServerException | IOException ex) {
            log.error("Error processing {}", job, ex);
            queueFail(dao, job, ex.getMessage());
        }
    }

    private QueueJob dequeueJob(final RawRepoDAO dao) throws RawRepoException {
        final QueueJob job = dao.dequeue(WORKER);
        return job;
    }

    private Record fetchRecord(RawRepoDAO dao, String id, int library) throws RawRepoException, MarcXMergerException {
        MarcXMerger merger = null;
        try {
            if (!dao.recordExistsMaybeDeleted(id, library)) {
                return null;
            }
            merger = mergerPool.getMerger();
            return dao.fetchMergedRecordExpanded(id, library, merger, true);
        } finally {
            if (merger != null) {
                mergerPool.putMerger(merger);
            }
        }
    }

    private String createSolrDocumentId(RecordId recordId) {
        return recordId.getBibliographicRecordId() + ":" + recordId.getAgencyId();
    }

    SolrInputDocument createIndexDocument(Record record) {
        final SolrInputDocument doc = new SolrInputDocument();
        RecordId recordId = record.getId();
        doc.addField("id", createSolrDocumentId(recordId));

        String mimeType = record.getMimeType();
        switch (mimeType) {
            case MarcXChangeMimeType.MARCXCHANGE:
            case MarcXChangeMimeType.ARTICLE:
            case MarcXChangeMimeType.AUTHORITY:
            case MarcXChangeMimeType.ENRICHMENT:
                log.debug("Indexing content of {} with mimetype {}", recordId, mimeType);
                String content = new String(record.getContent(), StandardCharsets.UTF_8);
                try {
                    worker.addFields(doc, content, mimeType);
                } catch (Exception ex) {
                    log.error("Error adding fields for document '{}': ", content, ex);
                }
                break;
            default:
                log.debug("Skipping indexing of {} with mimetype {}", recordId, mimeType);
        }

        doc.addField("rec.bibliographicRecordId", recordId.getBibliographicRecordId());
        doc.addField("rec.agencyId", recordId.getAgencyId());
        doc.addField("rec.created", record.getCreated());
        doc.addField("rec.modified", record.getModified());
        doc.addField("rec.trackingId", record.getTrackingId());
        log.trace("Created solr document {}", doc);
        return doc;
    }

    private void deleteSolrDocument(RecordId jobId) throws IOException, SolrServerException {
        log.debug("Deleting document for {} to solr", jobId);
        solrServer.deleteById(createSolrDocumentId(jobId));
    }

    private void updateSolr(RecordId jobId, SolrInputDocument doc) throws IOException, SolrServerException {
        log.debug("Adding document for {} to solr", jobId);
        solrServer.add(doc);
    }

    private void queueFail(RawRepoDAO dao, QueueJob job, String error) throws RawRepoException {
        dao.queueFail(job, error);
    }

    private void commit(final Connection connection) throws SQLException {
        connection.commit();
    }

    private static String createTrackingId(QueueJob job) {
        return "RawRepoIndexer:" + job.toString();
    }

    private static String createTrackingId(QueueJob job, Record record) {
        String trackingId = record.getTrackingId();
        if (trackingId == null || trackingId.isEmpty()) {
            return createTrackingId(job);
        } else {
            return createTrackingId(job) + "<" + trackingId;
        }
    }
}
