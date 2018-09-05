/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPL v3
 *  See license text at https://opensource.dbc.dk/licenses/gpl-3.0
 */

package dk.dbc.rawrepo.indexer;

import dk.dbc.rawrepo.dto.RecordDTO;
import dk.dbc.rawrepo.dto.RecordExistsDTO;
import dk.dbc.util.Stopwatch;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.TimeUnit;


@Stateless
public class RawRepoRecordBean {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(RawRepoRecordBean.class);
    private static final XLogger LOGGER_STOPWATCH = XLoggerFactory.getXLogger("dk.dbc.rawrepo.indexer.stopwatch");

    private final String URL_RECORD = "%s/api/v1/record/%s/%s?allow-deleted=true";
    private final String URL_RECORD_EXISTS = "%s/api/v1/record/%s/%s/exists?mode=merged&allow-deleted=true";

    @Inject
    @ConfigProperty(name = "RAWREPO_RECORD_URL", defaultValue = "RAWREPO_RECORD_URL not set")
    private String RAWREPO_RECORD_URL;

    private Client client;

    public boolean recordExistsMaybeDeleted(String id, int agencyId) {
        final Stopwatch stopwatch = new Stopwatch();

        final String uri = String.format(URL_RECORD_EXISTS, RAWREPO_RECORD_URL, agencyId, id);

        LOGGER.info("Calling {}", uri);

        client = ClientBuilder.newClient();
        final RecordExistsDTO dto = client.target(uri)
                .request(MediaType.APPLICATION_JSON)
                .get(RecordExistsDTO.class);

        LOGGER_STOPWATCH.info("recordExistsMaybeDeleted took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        return dto.isValue();
    }

    public RecordDTO fetchRecord(String id, int agencyId) {
        final Stopwatch stopwatch = new Stopwatch();

        RecordDTO record = null;

        final String uri = String.format(URL_RECORD, RAWREPO_RECORD_URL, agencyId, id);

        LOGGER.info("Calling {}", uri);

        client = ClientBuilder.newClient();
        record = client.target(uri)
                .request(MediaType.APPLICATION_JSON)
                .get(RecordDTO.class);

        LOGGER_STOPWATCH.info("fetchRecord took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        return record;
    }

}
