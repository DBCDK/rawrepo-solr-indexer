package dk.dbc.rawrepo.indexer;

import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IndexerTest {


    private class RecordDataTest extends RecordDTO {
        private String modifiedTest;
        private String createdTest;
        private String mimetypeTest;
        private RecordIdDTO recordIdTest;

        public RecordDataTest() {
            super();
        }

        public void setModified(String date) {
            this.modifiedTest = date;
        }

        @Override
        public String getModified() {
            return this.modifiedTest;
        }

        public void setCreated(String date) {
            this.createdTest = date;
        }

        @Override
        public String getCreated() {
            return this.createdTest;
        }

        public void setMimetype(String mimetype) {
            this.mimetypeTest = mimetype;
        }

        @Override
        public String getMimetype() {
            return this.mimetypeTest;
        }

        public void setRecordId( RecordIdDTO recordId ) {
            this.recordIdTest = recordId;
        }

        @Override
        public RecordIdDTO getRecordId() {
            return this.recordIdTest;
        }

    }

    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMdd")
            .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
            .toFormatter()
            .withZone(ZoneId.of("Europe/Copenhagen"));

    private RecordDTO createRecordData(String bibliographicRecordId,
                                      int agencyId,
                                      byte[] content,
                                      Instant created,
                                      Instant modified,
                                      boolean deleted,
                                      String mimetype) {
        RecordDTO recordData = new RecordDataTest();
        recordData.setContent(content);
        recordData.setCreated(created.toString());
        recordData.setModified(modified.toString());
        recordData.setDeleted(deleted);
        recordData.setMimetype(mimetype);
        recordData.setRecordId( new RecordIdDTO( bibliographicRecordId, agencyId) );
        return recordData;
    }

    private static Indexer createInstance() {
        @SuppressWarnings("UseInjectionInsteadOfInstantion")
        Indexer indexer = new Indexer();
        return indexer;
    }

    @Test
    public void testCreateIndexDocument() throws IOException, ParserConfigurationException, SAXException, Exception {

        Instant created = new Date(100).toInstant();
        Instant modified = new Date(200).toInstant();
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<marcx:record format=\"danMARC2\" type=\"Bibliographic\" xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n"
                + "      <marcx:leader>00000n    2200000   4500</marcx:leader>\n"
                + "      <marcx:datafield tag=\"001\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">2 364 149 6</marcx:subfield>\n"
                + "        <marcx:subfield code=\"b\">191919</marcx:subfield>\n"
                + "        <marcx:subfield code=\"c\">20130118221234</marcx:subfield>\n"
                + "        <marcx:subfield code=\"d\">20010822</marcx:subfield>\n"
                + "        <marcx:subfield code=\"f\">a</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"002\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">06605141</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"002\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"b\">810010</marcx:subfield>\n"
                + "        <marcx:subfield code=\"d\">09009310</marcx:subfield>\n"
                + "        <marcx:subfield code=\"x\">81001009009310</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"004\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"r\">n</marcx:subfield>\n"
                + "        <marcx:subfield code=\"a\">b</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"008\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"t\">m</marcx:subfield>\n"
                + "        <marcx:subfield code=\"u\">f</marcx:subfield>\n"
                + "        <marcx:subfield code=\"a\">2001</marcx:subfield>\n"
                + "        <marcx:subfield code=\"l\">dan</marcx:subfield>\n"
                + "        <marcx:subfield code=\"v\">0</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"014\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">2 364 143 7</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"021\">\n"
                + "        <marcx:subfield code=\"e\">9788777248573</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"022\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">0904-6054</marcx:subfield>\n"
                + "        <marcx:subfield code=\"c\">hf.</marcx:subfield>\n"
                + "        <marcx:subfield code=\"d\">kr. 6,50 pr. nr.</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"023\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">23-a</marcx:subfield>\n"
                + "        <marcx:subfield code=\"b\">23-b</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"032\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">IDO200137</marcx:subfield>\n"
                + "        <marcx:subfield code=\"x\">NET200137</marcx:subfield>\n"
                + "        <marcx:subfield code=\"x\">DAT991304</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"245\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"g\">1.3</marcx:subfield>\n"
                + "        <marcx:subfield code=\"a\">Forandringsledelse og orkestrering</marcx:subfield>\n"
                + "        <marcx:subfield code=\"e\">udarbejdet af: Danmarks Tekniske Universitet, Byg.DTU og Institut for Produktion og Ledelse</marcx:subfield>\n"
                + "        <marcx:subfield code=\"e\">forfatter: Peter Vogelius, Christian Koch</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"526\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"i\">Hertil findes</marcx:subfield>\n"
                + "        <marcx:subfield code=\"t\">Bilag</marcx:subfield>\n"
                + "        <marcx:subfield code=\"u\">http://www.arbejdsulykker.dk/pdf/1_3_bilag.pdf</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"532\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">Med litteraturhenvisninger</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"700\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"0\"/>\n"
                + "        <marcx:subfield code=\"a\">Vogelius</marcx:subfield>\n"
                + "        <marcx:subfield code=\"h\">Peter</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"700\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"0\"/>\n"
                + "        <marcx:subfield code=\"a\">Koch</marcx:subfield>\n"
                + "        <marcx:subfield code=\"h\">Christian</marcx:subfield>\n"
                + "        <marcx:subfield code=\"c\">f. 1958</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"710\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"Ã¥\">2</marcx:subfield>\n"
                + "        <marcx:subfield code=\"a\">Danmarks Tekniske Universitet</marcx:subfield>\n"
                + "        <marcx:subfield code=\"c\">BYG. DTU</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"710\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"Ã¥\">1</marcx:subfield>\n"
                + "        <marcx:subfield code=\"a\">Danmarks Tekniske Universitet</marcx:subfield>\n"
                + "        <marcx:subfield code=\"c\">Institut for Produktion og Ledelse</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"856\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"z\">AdgangsmÃ¥de: Internet</marcx:subfield>\n"
                + "        <marcx:subfield code=\"u\">http://www.arbejdsulykker.dk/pdf/met_1_3.pdf</marcx:subfield>\n"
                + "        <marcx:subfield code=\"z\">KrÃ¦ver lÃ¦seprogrammet Acrobat Reader</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"910\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">BYG. DTU, Danmarks Tekniske Universitet</marcx:subfield>\n"
                + "        <marcx:subfield code=\"z\">710/2</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"910\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">DTU</marcx:subfield>\n"
                + "        <marcx:subfield code=\"z\">710/1(a)</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"910\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">Institut for Produktion og Ledelse, Danmarks Tekniske Universitet</marcx:subfield>\n"
                + "        <marcx:subfield code=\"z\">710/1</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"910\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">IPL</marcx:subfield>\n"
                + "        <marcx:subfield code=\"z\">710/1</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"d08\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">tb</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"d08\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">rettet i forb. med tilf. af 008w</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"s10\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">DBC</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"z99\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">masseret</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "      <marcx:datafield tag=\"n55\" ind1=\"0\" ind2=\"0\">\n"
                + "        <marcx:subfield code=\"a\">20050302</marcx:subfield>\n"
                + "      </marcx:datafield>\n"
                + "    </marcx:record>";

        RecordDTO record = createRecordData("id", 123456, content.getBytes(), created, modified, true, Indexer.MIMETYPE_MARCXCHANGE);

        Indexer indexer = createInstance();
        indexer.worker = new JavaScriptWorker();

        SolrInputDocument doc = indexer.createIndexDocument(record);

        assertEquals(created, Instant.parse(doc.getField("rec.created").getValue().toString()));
        assertEquals(modified, Instant.parse(doc.getField("rec.modified").getValue().toString()));
        assertEquals("id:123456", doc.getField("id").getValue());
        assertEquals("id", doc.getField("rec.bibliographicRecordId").getValue());
        assertEquals(123456, doc.getField("rec.agencyId").getValue());

        // check that Marcx record is indexed correctly
        String field002a = (String) doc.getField("marc.002a").getValue();
        String field002x = (String) doc.getField("marc.002x").getValue();
        String field022a = (String) doc.getField("marc.022a").getValue();

        assertEquals("06605141", field002a);
        assertEquals("81001009009310", field002x);
        assertEquals("0904-6054", field022a);

    }

    @Test
    public void testCreateIndexDocument_whenContentCanNotBeParsed() throws IOException, ParserConfigurationException, SAXException, Exception {

        Instant created = new Date(100).toInstant();
        Instant modified = new Date(200).toInstant();
        String content = ">hello world<";

        RecordDTO record = createRecordData("id", 123456, content.getBytes(), created, modified, true, Indexer.MIMETYPE_MARCXCHANGE);

        Indexer indexer = createInstance();
        indexer.worker = new JavaScriptWorker();

        SolrInputDocument doc = indexer.createIndexDocument(record);

        assertEquals(created, Instant.parse(doc.getField("rec.created").getValue().toString()));
        assertEquals(modified, Instant.parse(doc.getField("rec.modified").getValue().toString()));
        assertEquals("id:123456", doc.getField("id").getValue());
        assertEquals("id", doc.getField("rec.bibliographicRecordId").getValue());
        assertEquals(123456, doc.getField("rec.agencyId").getValue());

        // check that Marcx record is not indexed
        assertNull("marc.002a is not present", doc.getField("marc.002a"));
        assertNull("marc.021ae is not present", doc.getField("marc.021ae"));
        assertNull("marc.022a is not present", doc.getField("marc.022a"));
    }

    @Test
    public void testCreateIndexDocument_whenDocumentIsNotMarc() throws IOException, ParserConfigurationException, SAXException, Exception {

        Instant created = new Date(100).toInstant();
        Instant modified = new Date(200).toInstant();
        String content = "";

        RecordDTO record = createRecordData("id", 123456, content.getBytes(), created, modified, true, "DUMMY");

        Indexer indexer = createInstance();
        indexer.worker = new JavaScriptWorker();

        SolrInputDocument doc = indexer.createIndexDocument(record);

        assertEquals(created, Instant.parse(doc.getField("rec.created").getValue().toString()));
        assertEquals(modified, Instant.parse(doc.getField("rec.modified").getValue().toString()));
        assertEquals("id:123456", doc.getField("id").getValue());
        assertEquals("id", doc.getField("rec.bibliographicRecordId").getValue());
        assertEquals(123456, doc.getField("rec.agencyId").getValue());

        assertNull("marc.002a is not present", doc.getField("marc.002a"));
        assertNull("marc.021ae is not present", doc.getField("marc.021ae"));
        assertNull("marc.022a is not present", doc.getField("marc.022a"));
    }

}
