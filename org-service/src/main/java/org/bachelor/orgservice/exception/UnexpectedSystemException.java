package org.bachelor.orgservice.exception;

public class UnexpectedSystemException extends RuntimeException {
    public UnexpectedSystemException() {
        super("Something went wrong. Please try again later");
    }
}
