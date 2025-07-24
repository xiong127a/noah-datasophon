package com.datasophon.api.utils.ranger.client.utils;

import lombok.Getter;

import java.io.Serial;

public class RangerClientException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * -- GETTER --
     *  Gets the HTTP status code of the failure, such as 404.
     */
    @Getter
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
