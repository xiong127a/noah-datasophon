package com.datasophon.api.utils.ranger.client.utils;

import java.io.Serial;

public class RangerClientException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private Throwable cause;
    private final int status;
    private final String message;

    public RangerClientException(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public RangerClientException(String message) {
        this.status = 500;
        this.message = message;
    }

    public RangerClientException(String message, Throwable cause) {
        super(message, cause);
        this.status = 500;
        this.message = message;
        this.cause = cause;
    }

    /**
     * Gets the HTTP status code of the failure, such as 404.
     */
    public int getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message + " (http status: " + status + ")";
    }

    @Override
    public String toString() {
        return message + " (http status: " + status + ")";
    }
}
