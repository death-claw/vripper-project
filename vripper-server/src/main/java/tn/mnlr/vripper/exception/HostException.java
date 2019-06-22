package tn.mnlr.vripper.exception;

public class HostException extends Exception {
    public HostException(Exception e) {
        super(e);
    }

    public HostException(String message) {
        super(message);
    }

    public HostException(String message, Exception e) {
        super(message,e);
    }
}
