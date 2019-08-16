package tn.mnlr.vripper.exception;

public class PostParseException extends Exception {

    public PostParseException(String message) {
        super(message);
    }

    public PostParseException(String message, Exception e) {
        super(message, e);
    }

    public PostParseException(Exception e) {
        super(e);
    }
}
