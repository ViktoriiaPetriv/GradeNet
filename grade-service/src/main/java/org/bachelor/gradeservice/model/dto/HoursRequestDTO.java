package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.NotNull;

public record HoursRequestDTO(
        @NotNull Long specialtyDisciplineId,
        Integer ectsHours,
        Integer allHours,
        Integer totalClassroomHours,
        Integer lecture,
        Integer seminar,
        Integer laboratory,
        Integer individual,
        Integer selfWork
) {}
