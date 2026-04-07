package org.bachelor.gradeservice.model.dto;


import java.time.Instant;

public record DisciplineDTO(
        Long id,
        String name,
        Long specialtyId,
        Long professorId,
        Instant reportDate
) {}
