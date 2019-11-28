package de.hhn.aib.swlab.ex3.server.singlebackend.internal.exception;

public class WebSocketException extends RuntimeException {
    public WebSocketException() {
    }

    public WebSocketException(String message) {
        super(message);
    }

    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketException(Throwable cause) {
        super(cause);
    }

    public WebSocketException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
