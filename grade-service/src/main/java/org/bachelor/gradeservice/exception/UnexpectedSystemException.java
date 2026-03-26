package org.bachelor.gradeservice.exception;

public class UnexpectedSystemException extends RuntimeException {
    public UnexpectedSystemException() {
        super("Щось пішло не так. Будь ласка, зверніться до служби підтримки.");
    }
}
