package org.bachelor.orgservice.model.dto;

public record OrgInfoDTO(
        Long facultyId,
        String facultyName,
        Long departmentId,
        String departmentName
) {}