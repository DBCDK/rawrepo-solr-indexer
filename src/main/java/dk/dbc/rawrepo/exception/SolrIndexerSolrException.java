package dk.dbc.rawrepo.exception;

// The class is named as it is in order to avoid confusion with the existing SolrException
public class SolrIndexerSolrException extends Exception {

    public SolrIndexerSolrException(String message, Throwable cause) {
        super(message, cause);
    }

}
