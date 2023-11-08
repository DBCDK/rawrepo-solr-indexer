package dk.dbc.rawrepo.exception;

// The class is named as it is in order to avoid confusion with the existing RawRepoException
public class SolrIndexerRawRepoException extends Exception {

    public SolrIndexerRawRepoException() {
    }

    public SolrIndexerRawRepoException(String message) {
        super(message);
    }

    public SolrIndexerRawRepoException(Throwable cause) {
        super(cause);
    }

    public SolrIndexerRawRepoException(String message, Throwable cause) {
        super(message, cause);
    }

}
