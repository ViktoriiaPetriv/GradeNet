package org.bachelor.gradeservice.exception;

public class RestException extends RuntimeException {
    public RestException(String message) {
        super(message);
    }
}
