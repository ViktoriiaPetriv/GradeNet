package org.bachelor.addservice.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PracticeDetailsDTO {
    private Long id;
    private Long additionalWorkId;
    private String organization;
    private Integer course;
    private LocalDate startDate;
    private LocalDate endDate;
    private String workDescription;
    private Integer ectsCredits;
    private Integer totalHours;
    private Long supervisorId;
}
