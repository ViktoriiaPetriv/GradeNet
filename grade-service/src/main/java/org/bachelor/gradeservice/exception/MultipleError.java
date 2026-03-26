package org.bachelor.gradeservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MultipleError {
    private List<ErrorResponse> errors;
}
