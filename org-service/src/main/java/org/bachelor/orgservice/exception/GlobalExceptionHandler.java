package org.bachelor.orgservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MultipleError> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(field -> new ErrorResponse(HttpStatus.BAD_REQUEST.value(), field.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MultipleError(errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        return handleUnexpectedSystemException(new UnexpectedSystemException());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(UnexpectedSystemException.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedSystemException(UnexpectedSystemException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityExistsException(EntityExistsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(RestException.class)
    public ResponseEntity<ErrorResponse> handleRestException(RestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }
}
