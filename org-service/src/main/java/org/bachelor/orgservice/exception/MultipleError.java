package org.bachelor.orgservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MultipleError {
    private List<ErrorResponse> errors;
}
