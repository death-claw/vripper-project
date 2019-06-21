package tn.mnlr.vripper.exception;

public class DownloadException extends Exception {

    public DownloadException(String message) {
        super(message);
    }

    public DownloadException(Exception e) {
        super(e);
    }
}
