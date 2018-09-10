/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.indexer;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * @author DBC {@literal <dbc.dk>}
 */
@RunWith(Parameterized.class)
public class JavaScriptWorkerTest {

    private static final XLogger LOGGER = XLoggerFactory.getXLogger(JavaScriptWorkerTest.class);

    private final String dir;
    private final File path;

    public JavaScriptWorkerTest(String dir, String path) {
        this.dir = dir;
        this.path = new File(path);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection getContent() throws Exception {
        ArrayList<String[]> list = new ArrayList<>();
        String name = JavaScriptWorkerTest.class.getSimpleName();
        ClassLoader classLoader = JavaScriptWorkerTest.class.getClassLoader();
        URL resource = classLoader.getResource(name);
        if (resource == null || !resource.getProtocol().equals("file")) {
            throw new Exception("Cannot find catalog for: " + name);
        }
        File file = new File(resource.getPath());
        if (!file.isDirectory()) {
            throw new Exception("Cannot find catalog for: " + name + " not a directory");
        }
        File[] dirs = file.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        for (File dir : dirs) {
            list.add(new String[]{dir.getName(), dir.getPath()});
        }
        return list;
    }

    @Test
    public void testAddFields() throws Exception {
        String content = getContent(new File(path, "record"));
        String mimetype = getContent(new File(path, "mimetype")).trim();
        String expected = getContent(new File(path, "expected"));

        HashMap<String, HashSet<String>> collection = new HashMap<>();
        SolrInputDocument sid = mockSolrInputDocument(collection);

        JavaScriptWorker jsw = new JavaScriptWorker();

        jsw.addFields(sid, content, mimetype);

        validate(expected, collection);
    }

    private void validate(String expected, HashMap<String, HashSet<String>> actual) {
        HashMap<String, HashSet<String>> missing = new HashMap<>();
        JsonReader reader = Json.createReader(new StringReader(expected));
        JsonObject obj = reader.readObject();
        for (String key : obj.keySet()) {
            HashSet<String> set = actual.get(key);
            if (set == null) {
                HashSet<String> more = new HashSet<>();
                missing.put(key, more);
                JsonArray array = obj.getJsonArray(key);
                for (int i = 0; i < array.size(); i++) {
                    more.add(array.getString(i));
                }
            } else {
                HashSet<String> more = new HashSet<>();
                JsonArray array = obj.getJsonArray(key);
                for (int i = 0; i < array.size(); i++) {
                    String string = array.getString(i);
                    if (!set.remove(string)) {
                        more.add(string);
                    }
                }
                if (!more.isEmpty()) {
                    missing.put(key, more);
                }
                if (set.isEmpty()) {
                    actual.remove(key);
                }
            }
        }
        if (actual.isEmpty() && missing.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Content error: ");
        if (!actual.isEmpty()) {
            sb.append("has extra: ").append(actual);
        }
        if (!actual.isEmpty() && !missing.isEmpty()) {
            sb.append(" ");
        }
        if (!missing.isEmpty()) {
            sb.append("missing: ").append(missing);
        }
        fail(sb.toString());
    }

    private SolrInputDocument mockSolrInputDocument(final HashMap<String, HashSet<String>> collection) {
        SolrInputDocument sid = mock(SolrInputDocument.class);
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                String name = (String) arguments[0];
                String value = (String) arguments[1];
                LOGGER.debug("mock: name = " + name + "; value = " + value);
                HashSet<String> set = collection.computeIfAbsent(name, k -> new HashSet<>());
                set.add(value);
                return null;
            }
        }).when(sid).addField(anyString(), anyString());
        return sid;
    }

    private String getContent(File file) throws MalformedURLException, IOException {
        InputStream stream = file.toURI().toURL().openStream();
        int available = stream.available();
        byte[] bytes = new byte[available];
        stream.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
