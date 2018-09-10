/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.indexer;

public class IndexerIT {
 /*
    Out-commenting this class as it can no longer be run - however we still need to test what was being tested somehow

    private static final String PROVIDER = "test";
    private static final String WORKER = "changed";

    private static final String BIBLIOGRAPHIC_RECORD_ID = "A";
    private static final int AGENCY_ID = 191919;

    private static final String OPENAGENCY_URL = "http://openagency.addi.dk/2.33/";

    String jdbcUrl;
    private Connection connection;

    SolrServer solrServer;
    String solrServerUrl;

    @Before
    public void setUp() throws Exception {
        String port = System.getProperty("postgresql.port");
        jdbcUrl = "jdbc:postgresql://localhost:" + port + "/rawrepo";
        connection = DriverManager.getConnection(jdbcUrl);
        connection.setAutoCommit(false);
        resetDatabase();

        solrServerUrl = "http://localhost:" + System.getProperty("jetty.port") + "/solr/raw-repo-index";
        solrServer = new HttpSolrServer(solrServerUrl);
    }

    @After
    public void tearDown() throws Exception {
        solrServer.deleteByQuery("*:*", 0);
        solrServer.commit(true, true);
        connection.close();
    }

    private void resetDatabase() throws SQLException {
        connection.prepareStatement("DELETE FROM relations").execute();
        connection.prepareStatement("DELETE FROM records").execute();
        connection.prepareStatement("DELETE FROM queue").execute();
        connection.prepareStatement("DELETE FROM queuerules").execute();
        connection.prepareStatement("DELETE FROM queueworkers").execute();

        PreparedStatement stmt = connection.prepareStatement("INSERT INTO queueworkers(worker) VALUES(?)");
        stmt.setString(1, WORKER);
        stmt.execute();

        stmt = connection.prepareStatement("INSERT INTO queuerules(provider, worker, changed, leaf) VALUES('" + PROVIDER + "', ?, ?, ?)");
        stmt.setString(1, WORKER);
        stmt.setString(2, "Y");
        stmt.setString(3, BIBLIOGRAPHIC_RECORD_ID);
        stmt.execute();
    }

    private Indexer createInstance() throws SQLException {
        Indexer indexer = createInstance(solrServerUrl);
        return indexer;
    }

    private Indexer createInstance(String solrUrl) throws SQLException {
        Indexer indexer = new Indexer();
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl);
        indexer.rawrepoDataSource = dataSource;
        indexer.openAgencyUrl = "http://openagency.addi.dk/2.33/";


        indexer.SOLR_URL = solrUrl;
        indexer.WORKER = WORKER;
        indexer.create();
        return indexer;
    }

    @Test
    public void createRecord() throws Exception {
        RawRepoQueueDAO dao = RawRepoQueueDAO.builder(connection).build();
        assertFalse(dao.recordExists(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID));

        RecordDTO record1 = dao.fetchRecord(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID);
        record1.setContent("First edition".getBytes());
        record1.setMimetype(MarcXChangeMimeType.MARCXCHANGE);
        dao.saveRecord(record1);
        assertTrue(dao.recordExists(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID));
        dao.changedRecord(PROVIDER, record1.getId());
        connection.commit();
        Indexer indexer = createInstance();
        indexer.performWork();
        solrServer.commit(true, true);

        QueryResponse response = solrServer.query(new SolrQuery("rec.agencyId:" + AGENCY_ID));
        assertEquals("Document can be found using library no.", 1, response.getResults().getNumFound());

        Date indexedDate = (Date) response.getResults().get(0).getFieldValue("rec.indexedDate");
        assertTrue("Field 'indexedDate' has been properly set", indexedDate != null);

        response = solrServer.query(new SolrQuery("marc.001b:870971"));
        assertEquals("Document can not be found using different library no.", 0, response.getResults().getNumFound());
    }

    @Test
    public void deleteRecord() throws Exception {
        RawRepoDAO dao = RawRepoDAO.builder(connection).relationHints(new RelationHintsOpenAgency(OpenAgencyServiceFromURL.builder().build(OPENAGENCY_URL))).build();
        assertFalse(dao.recordExists(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID));

        // Put in a document that is going to be deleted
        SolrInputDocument document = new SolrInputDocument();
        document.addField("id", BIBLIOGRAPHIC_RECORD_ID + ":" + AGENCY_ID);
        solrServer.add(document);

        Record record = dao.fetchRecord(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID);
        record.setMimeType(MarcXChangeMimeType.MARCXCHANGE);
        record.setDeleted(true);
        dao.saveRecord(record);
        dao.changedRecord(PROVIDER, record.getId());
        assertTrue("Record exists", dao.recordExistsMaybeDeleted(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID));
        assertFalse("Record is deleted", dao.recordExists(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID));

        connection.commit();

        Indexer indexer = createInstance();
        indexer.performWork();
        solrServer.commit(true, true);

        QueryResponse response = solrServer.query(new SolrQuery("id:" + BIBLIOGRAPHIC_RECORD_ID + "\\:" + AGENCY_ID));
        assertEquals("Document can not be found using id. Must have been deleted", 0, response.getResults().getNumFound());
    }

    @Test(expected = SolrIndexerSolrException.class)
    public void createRecordWhenIndexingFails() throws Exception {
        RawRepoDAO dao = RawRepoDAO.builder(connection).relationHints(new RelationHintsOpenAgency(OpenAgencyServiceFromURL.builder().build(OPENAGENCY_URL))).build();
        assertFalse(dao.recordExists(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID));

        Record record1 = dao.fetchRecord(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID);
        record1.setContent("First edition".getBytes());
        record1.setMimeType(MarcXChangeMimeType.MARCXCHANGE);
        dao.saveRecord(record1);
        assertTrue(dao.recordExists(BIBLIOGRAPHIC_RECORD_ID, AGENCY_ID));
        dao.changedRecord(PROVIDER, record1.getId());
        connection.commit();

        Indexer indexer = createInstance(solrServerUrl + "X");
        indexer.performWork();
        solrServer.commit(true, true);
    }
    */
}
