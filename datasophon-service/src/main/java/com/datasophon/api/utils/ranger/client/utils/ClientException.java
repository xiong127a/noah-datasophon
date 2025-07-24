package com.datasophon.api.utils.ranger.client.utils;

import java.io.Serial;

public class ClientException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public ClientException() {
    }

    /**
     * @param message to throw
     * @param cause   to throw
     */
    public ClientException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message to throw
     */
    public ClientException(final String message) {
        super(message);
    }

    /**
     * @param cause to throw
     */
    public ClientException(final Throwable cause) {
        super(cause);
    }
}
