package org.bachelor.gradeservice.model.dto;

public record HoursDTO(
        Long id,
        Long specialtyDisciplineId,
        Integer ectsHours,
        Integer allHours,
        Integer totalClassroomHours,
        Integer lecture,
        Integer seminar,
        Integer laboratory,
        Integer individual,
        Integer selfWork
) {}
