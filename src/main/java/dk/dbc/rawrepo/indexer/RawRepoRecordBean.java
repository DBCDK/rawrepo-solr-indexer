/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.indexer;

import dk.dbc.rawrepo.RecordData;
import dk.dbc.rawrepo.RecordServiceConnector;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.util.Stopwatch;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import java.util.concurrent.TimeUnit;


@Stateless
public class RawRepoRecordBean {
    private static final XLogger LOGGER_STOPWATCH = XLoggerFactory.getXLogger("dk.dbc.rawrepo.indexer.stopwatch");

    @Inject
    @ConfigProperty(name = "RAWREPO_RECORD_URL", defaultValue = "RAWREPO_RECORD_URL not set")
    private String RAWREPO_RECORD_URL;

    private RecordServiceConnector recordServiceConnector;

    @PostConstruct
    public void initialize() {
        recordServiceConnector = new RecordServiceConnector(ClientBuilder.newClient(), RAWREPO_RECORD_URL);
    }

    @PreDestroy
    public void close() {
        if (recordServiceConnector != null) {
            recordServiceConnector.close();
        }
    }

    public boolean recordExistsMaybeDeleted(String id, int agencyId) throws RecordServiceConnectorException {
        return recordServiceConnector.recordExists(agencyId, id);
    }

    public RecordData fetchRecord(String id, int agencyId) throws RecordServiceConnectorException {
        final Stopwatch stopwatch = new Stopwatch();

        RecordData record = recordServiceConnector.getRecordData(agencyId, id);

        LOGGER_STOPWATCH.info("fetchRecord took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        return record;
    }

}
