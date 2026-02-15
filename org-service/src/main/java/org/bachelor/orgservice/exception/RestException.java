package org.bachelor.orgservice.exception;

public class RestException extends RuntimeException {
    public RestException(String message) {
        super(message);
    }
}
