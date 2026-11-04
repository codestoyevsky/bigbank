package eu.bigbank.mugloar.client;

/** {@code status} is the game API's HTTP status, or 0 if it could not be reached. */
public class MugloarApiException extends RuntimeException {

    private final int status;

    public MugloarApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public MugloarApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    public int status() {
        return status;
    }

    public boolean isUnreachable() {
        return status == 0 || status >= 500;
    }

    public boolean isGameOver() {
        return status == 410;
    }
}
