package dk.dbc.rawrepo.exception;

// The class is named as it is in order to avoid confusion with the existing SolrException
public class SolrIndexerSolrException extends Exception {

    public SolrIndexerSolrException() {
    }

    public SolrIndexerSolrException(String message) {
        super(message);
    }

    public SolrIndexerSolrException(Throwable cause) {
        super(cause);
    }

    public SolrIndexerSolrException(String message, Throwable cause) {
        super(message, cause);
    }

}
