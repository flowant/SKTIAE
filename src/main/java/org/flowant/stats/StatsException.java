package org.flowant.stats;

public class StatsException extends Exception {
    private static final long serialVersionUID = -7176817350159024406L;

    public StatsException() {
        super();
    }

    public StatsException(String message) {
        super(message);
    }

    public StatsException(String message, Throwable cause) {
        super(message, cause);
    }

    public StatsException(Throwable cause) {
        super(cause);
    }

    protected StatsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}