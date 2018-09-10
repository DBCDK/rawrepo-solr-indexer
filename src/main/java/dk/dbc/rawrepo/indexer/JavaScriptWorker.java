/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.indexer;

import dk.dbc.jslib.ClasspathSchemeHandler;
import dk.dbc.jslib.Environment;
import dk.dbc.jslib.ModuleHandler;
import dk.dbc.jslib.SchemeURI;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * @author DBC {@literal <dk.dbc.dk>}
 */
public class JavaScriptWorker {

    private static final Logger log = LoggerFactory.getLogger(JavaScriptWorker.class);
    private static final String INDEXER_SCRIPT = "indexer.js";
    private static final String INDEXER_METHOD = "index";

    /**
     * Std search path
     */
    private static final String[] searchPaths = new String[]{
            "classpath:javascript/",
            "classpath:javascript/javacore/",
            "classpath:javascript/jscommon/config/",
            "classpath:javascript/jscommon/convert/",
            "classpath:javascript/jscommon/devel/",
            "classpath:javascript/jscommon/external/",
            "classpath:javascript/jscommon/io/",
            "classpath:javascript/jscommon/marc/",
            "classpath:javascript/jscommon/net/",
            "classpath:javascript/jscommon/system/",
            "classpath:javascript/jscommon/util/",
            "classpath:javascript/jscommon/xml/"
    };

    private final Environment internal_indexes_env;

    public JavaScriptWorker() {
        try {
            internal_indexes_env = new Environment();
            ModuleHandler mh = new ModuleHandler();
            mh.registerNonCompilableModule("Tables"); // Unlikely we need this module.

            // Builtin searchpath
            SolrFieldsSchemeHandler solrFields = new SolrFieldsSchemeHandler(this);
            mh.registerHandler(SolrFieldsSchemeHandler.SOLR_FIELDS_SCHEME, solrFields);
            mh.addSearchPath(new SchemeURI(SolrFieldsSchemeHandler.SOLR_FIELDS_SCHEME + ":"));

            // Classpath searchpath
            ClasspathSchemeHandler classpath = new ClasspathSchemeHandler(getClass().getClassLoader());
            mh.registerHandler("classpath", classpath);
            for (String searchPath : searchPaths) {
                mh.addSearchPath(new SchemeURI(searchPath));
            }

            // Use system
            internal_indexes_env.registerUseFunction(mh);

            // Evaluate script
            InputStream stream = getClass().getClassLoader().getResourceAsStream(INDEXER_SCRIPT);
            InputStreamReader inputStreamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);

            internal_indexes_env.eval(inputStreamReader, INDEXER_SCRIPT);
        } catch (Exception ex) {
            log.error("Error initializing javascript", ex);
            throw new RuntimeException("Cannot initlialize javascript", ex);
        }
    }

    /**
     * member variable exposed to javascript
     */
    private SolrInputDocument solrInputDocument;

    /**
     * JavaScript exposed method - adds field/value to solrInputDocument
     *
     * @param name
     * @param value
     */
    public void addField(String name, String value) {
        solrInputDocument.addField(name, value);
        log.trace("Adding " + name + ": " + value);
    }

    /**
     * Run script on content, adding data to solrInputDocument
     *
     * @param solrInputDocument target
     * @param content           String containing marcxchange
     * @param mimetype          mimetype of marcxchange
     * @throws Exception
     */
    void addFields(SolrInputDocument solrInputDocument, String content, String mimetype) throws Exception {
        this.solrInputDocument = solrInputDocument;

        internal_indexes_env.callMethod(INDEXER_METHOD, new Object[]{content, mimetype});
    }
}
