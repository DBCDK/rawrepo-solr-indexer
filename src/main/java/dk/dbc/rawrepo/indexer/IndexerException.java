package dk.dbc.rawrepo.indexer;

public class IndexerException extends Exception {

        /**
    *
    */
    private static final long serialVersionUID = -7670152284752524062L;

    public IndexerException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexerException(Throwable cause) {
        super(cause);
    }


}
