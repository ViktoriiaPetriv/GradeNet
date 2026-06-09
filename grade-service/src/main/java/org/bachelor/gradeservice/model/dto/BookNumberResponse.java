package org.bachelor.gradeservice.model.dto;

public record BookNumberResponse(
        Long id,
        String number,
        Long studentId,
        String studentFirstName,
        String studentLastName
) {}
