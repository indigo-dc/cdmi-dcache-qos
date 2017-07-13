package org.dcache.spi.exception;

import java.net.URI;

public class SpiException extends Exception {
    private static final long serialVersionUID = 1L;

    public SpiException(String message) {
        super(message);
    }

    public SpiException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpiException(Throwable cause) {
        super(cause);
    }

    public SpiException(URI url, String method, String message) {
        super("Fail " + method.toUpperCase() + " on (" + url + "): " + message);
    }
}
