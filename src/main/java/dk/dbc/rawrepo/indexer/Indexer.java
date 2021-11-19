package dk.dbc.rawrepo.indexer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.MDC;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author DBC {@literal <dk.dbc.dk>}
 */
@Stateless
public class Indexer {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(Indexer.class);
    private static final XLogger LOGGER_STOPWATCH = XLoggerFactory.getXLogger("dk.dbc.rawrepo.indexer.stopwatch");

    private static final String TRACKING_ID = "trackingId";

    @Inject
    @ConfigProperty(name = "SOLR_URL", defaultValue = "SOLR_URL not set")
    protected String SOLR_URL;

    @Inject
    @ConfigProperty(name = "WORKER", defaultValue = "WORKER not set")
    protected String WORKER;

    @Resource(lookup = "jdbc/rawrepo")
    protected DataSource rawrepoDataSource;

    @Inject
    private RecordServiceConnector recordServiceConnector;

    @EJB
    private RawRepoQueueBean queueBean;

    static final String MIMETYPE_MARCXCHANGE = "text/marcxchange";

    protected JSEnvironment jsEnvironment;

    private SolrServer solrServer;
    private final ObjectMapper mapper = new ObjectMapper();
    // Object used to map JavaScript object back to Java object
    private final TypeReference<Map<String, List<String>>> typeRef = new TypeReference<Map<String, List<String>>>() {
    };

    public Indexer() {
        this.solrServer = null;
    }


    @PostConstruct
    public void create() {
        try {
            solrServer = new HttpSolrServer(SOLR_URL);
            solrServer.ping();
        } catch (SolrServerException | IOException ex) {
            throw new IndexerRuntimeException("Exception during instantiation of Solr server connection", ex);
        }

        try {
            jsEnvironment = new JSEnvironment();
            jsEnvironment.init();
        } catch (Exception ex) {
            throw new IndexerRuntimeException("Exception during instantiation of JavaScript environment", ex);
        }
    }

    @PreDestroy
    public void destroy() {
        solrServer.shutdown();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void performWork() throws SolrIndexerRawRepoException, SolrIndexerSolrException, RecordServiceConnectorException {
        boolean moreWork = true;
        int processedJobs = 0;

        // Test connection and return proper error if there is a problem
        try {
            solrServer.ping();
        } catch (SolrServerException | IOException | HttpSolrServer.RemoteSolrException ex) {
            throw new SolrIndexerSolrException("Could not connect to the solr server. " + ex.getMessage(), ex);
        }

        while (moreWork) {
            try (Connection connection = getConnection()) {
                final RawRepoQueueDAO dao = createDAO(connection);
                try {
                    final QueueItem job = queueBean.dequeueJob(dao, WORKER);

                    if (job != null) {
                        MDC.put(TRACKING_ID, createTrackingId()); // Early trackingId as we don't yet have the record
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
    public void processJob(QueueItem job, RawRepoQueueDAO dao) throws QueueException, SolrIndexerSolrException, RecordServiceConnectorException {
        LOGGER.info("Indexing {}", job);
        final Stopwatch stopwatch = new Stopwatch();

        final RecordData.RecordId jobId = new RecordData.RecordId(job.getBibliographicRecordId(), job.getAgencyId());
        LOGGER.info("---------------------------------------------------------------");
        try {
            final RecordData recordData = fetchRecord(jobId);
            if (recordData == null) {
                LOGGER.info("record from {} does not exist, most likely queued by dependency", job);
                return;
            }
            MDC.put(TRACKING_ID, createTrackingId(recordData));
            if (recordData.isDeleted()) {
                deleteSolrDocument(jobId);
            } else {
                SolrInputDocument doc = createIndexDocument(recordData);
                updateSolr(jobId, doc);
            }
            LOGGER.info("Indexed {}", job);
        } catch (HttpSolrServer.RemoteSolrException ex) {
            // Index is missing on the solr server so we need to stop now
            if (ex.getMessage().contains("unknown field")) {
                throw new SolrIndexerSolrException("Missing index: " + ex.getMessage(), ex);
            }
            LOGGER.error("Error processing {}", job, ex);
            queueBean.queueFail(dao, job, ex.getMessage());
        } catch (SolrException | SolrServerException | IOException ex) {
            LOGGER.error("Error processing {}", job, ex);
            queueBean.queueFail(dao, job, ex.getMessage());
        } finally {
            MDC.remove(TRACKING_ID);
            LOGGER_STOPWATCH.info("processJob took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        }
    }

    private RecordData fetchRecord(RecordData.RecordId jobid) throws RecordServiceConnectorException {
        RecordServiceConnector.Params params = new RecordServiceConnector.Params();
        params.withAllowDeleted(true);

        if (!recordServiceConnector.recordExists(jobid.getAgencyId(), jobid.getBibliographicRecordId(), params)) {
            return null;
        } else {
            return recordServiceConnector.getRecordData(jobid.getAgencyId(), jobid.getBibliographicRecordId(), params);
        }
    }

    private String createSolrDocumentId(RecordData.RecordId recordId) {
        LOGGER.entry();
        String result = null;
        try {
            result = recordId.getBibliographicRecordId() + ":" + recordId.getAgencyId();

            return result;
        } finally {
            LOGGER.exit(result);
        }
    }

    SolrInputDocument createIndexDocument(RecordData recordData) {
        final SolrInputDocument doc = new SolrInputDocument();
        RecordData.RecordId recordId = recordData.getRecordId();
        doc.addField("id", createSolrDocumentId(recordId));

        String mimeType = recordData.getMimetype();
        LOGGER.debug("Indexing content of {} with mimetype {}", recordId, mimeType);
        String content = new String(recordData.getContent(), StandardCharsets.UTF_8);
        final SupplementaryData supplementaryData = new SupplementaryData();
        supplementaryData.setMimetype(recordData.getMimetype());
        supplementaryData.setCorrectedAgencyId(recordData.getRecordId().getAgencyId());

        try {
            Stopwatch stopwatch = new Stopwatch();

            final Object jsResult = jsEnvironment.getIndexes(content, supplementaryData);

            LOGGER_STOPWATCH.info("Javascript took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));

            if (jsResult != null) {
                final Map<String, List<String>> fields = mapper.readValue(jsResult.toString(), typeRef);

                final Iterator<Map.Entry<String, List<String>>> it = fields.entrySet().iterator();
                while (it.hasNext()) {
                    final Map.Entry<String, List<String>> pair = it.next();
                    doc.addField(pair.getKey(), pair.getValue());
                    it.remove(); // avoids a ConcurrentModificationException
                }
            } else {
                LOGGER.info("Got no useable result from js");
            }
        } catch (Exception ex) {
            LOGGER.error("Error adding fields for document '{}': ", content, ex);
        }

        doc.addField("rec.bibliographicRecordId", recordId.getBibliographicRecordId());
        doc.addField("rec.agencyId", recordId.getAgencyId());
        doc.addField("rec.created", recordData.getCreated());
        doc.addField("rec.modified", recordData.getModified());
        doc.addField("rec.trackingId", recordData.getTrackingId());
        LOGGER.trace("Created solr document {}", doc);
        return doc;
    }

    private void deleteSolrDocument(RecordData.RecordId jobId) throws IOException, SolrServerException {
        LOGGER.debug("Deleting document for {} to solr", jobId);
        solrServer.deleteById(createSolrDocumentId(jobId));
    }

    private void updateSolr(RecordData.RecordId jobId, SolrInputDocument doc) throws IOException, SolrServerException {
        LOGGER.debug("Adding document for {} to solr", jobId);
        Stopwatch stopwatch = new Stopwatch();
        solrServer.add(doc);
        LOGGER_STOPWATCH.info("updateSolr took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));

    }

    private void commit(final Connection connection) throws SQLException {
        connection.commit();
    }

    private static String createTrackingId() {
        return UUID.randomUUID().toString();
    }

    private static String createTrackingId(RecordData record) {
        String trackingId = record.getTrackingId();
        if (trackingId == null || trackingId.isEmpty()) {
            return createTrackingId();
        } else {
            return trackingId;
        }
    }
}
