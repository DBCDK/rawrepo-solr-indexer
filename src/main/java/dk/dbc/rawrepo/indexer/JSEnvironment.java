package dk.dbc.rawrepo.indexer;

import dk.dbc.jslib.helper.JavaScriptWrapperSingleEnvironment;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.util.Stopwatch;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.concurrent.TimeUnit;


public class JSEnvironment {
    private static final XLogger log = XLoggerFactory.getXLogger(JSEnvironment.class);
    private static final XLogger LOGGER_STOPWATCH = XLoggerFactory.getXLogger("dk.dbc.rawrepo.indexer.stopwatch");
    private static final String MODULE_SEARCH_PATH = "classpath:javascript/ classpath:javascript/javacore/ classpath:javascript/jscommon/system/ classpath:javascript/jscommon/convert/ classpath:javascript/jscommon/devel/ classpath:javascript/jscommon/util/ classpath:javascript/jscommon/external/ classpath:javascript/jscommon/marc/ classpath:javascript/jscommon/io/ classpath:javascript/jscommon/xml/ classpath:javascript/standard-index-values/ classpath:javascript/marc/ classpath:javascript/common/ classpath:javascript/validation/ classpath:javascript/jscommon/tables/ classpath:javascript/jscommon/net/ classpath:javascript/jscommon/config/";
    private static final String JAVA_SCRIPT_FILE = "javascript/entrypoint.js";
    private static final String JAVA_SCRIPT_FUNCTION = "index";

    private final JSONBContext jsonbContext = new JSONBContext();
    private JavaScriptWrapperSingleEnvironment wrapper;

    void init() throws IndexerException {
        log.info("Create javascript wrapper with file: {}, function: {}, search path{}", JAVA_SCRIPT_FILE, JAVA_SCRIPT_FUNCTION, MODULE_SEARCH_PATH);
        try {
            wrapper = new JavaScriptWrapperSingleEnvironment(MODULE_SEARCH_PATH, JAVA_SCRIPT_FILE);
        } catch (Exception ex) {
            log.error("Initializing {} failed", JAVA_SCRIPT_FILE);
            throw new IndexerException(ex);
        }
    }

    Object getIndexes(String content, SupplementaryData supplementaryData) throws IndexerException {
        Stopwatch stopwatch = new Stopwatch();

        try {
            return wrapper.callObjectFunction(JAVA_SCRIPT_FUNCTION, content, jsonbContext.marshall(supplementaryData));
        } catch (Exception e) {
            throw new IndexerException(e);
        } finally {
            LOGGER_STOPWATCH.info("{}|{}", "getIndexes", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        }
    }

}
