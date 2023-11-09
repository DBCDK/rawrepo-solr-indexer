package dk.dbc.rawrepo.indexer;

import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.queue.RawRepoQueueDAO;
import dk.dbc.util.Stopwatch;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import jakarta.ejb.Stateless;
import java.util.concurrent.TimeUnit;

@Stateless
public class RawRepoQueueBean {
    private static final XLogger LOGGER_STOPWATCH = XLoggerFactory.getXLogger("dk.dbc.rawrepo.indexer.stopwatch");

    public QueueItem dequeueJob(final RawRepoQueueDAO dao, String worker) throws QueueException {
        final Stopwatch stopwatch = new Stopwatch();
        final QueueItem job = dao.dequeue(worker);

        if (job != null) {
            LOGGER_STOPWATCH.info("dequeueJob took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
        }

        return job;
    }

    public void queueFail(final RawRepoQueueDAO dao, QueueItem job, String error) throws QueueException {
        final Stopwatch stopwatch = new Stopwatch();

        dao.queueFail(job, error);

        LOGGER_STOPWATCH.info("queueFail took {} ms", stopwatch.getElapsedTime(TimeUnit.MILLISECONDS));
    }
}
