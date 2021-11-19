package dk.dbc.rawrepo.indexer;

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;

/**
 * @author DBC {@literal <dk.dbc.dk>}
 */
@Stateless
public class Scheduler {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(Scheduler.class);

    @EJB
    private Indexer indexer;

    @Schedule(second = "*/5", minute = "*", hour = "*")
    public void performIndexing() {
        try {
            indexer.performWork();
        } catch (Exception e) {
            LOGGER.error("An unhandled exception has made its way to the scheduler which means something unrecoverable has happened, so aborting now");
            LOGGER.catching(e);
        }
    }
}
