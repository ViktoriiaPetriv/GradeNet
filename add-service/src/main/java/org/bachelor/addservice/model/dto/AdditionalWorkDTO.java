package org.bachelor.addservice.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdditionalWorkDTO {
    private Long id;
    private Long bookNumberId;
    private Long commissionId;
    private String type;
    private String title;
    private LocalDate eventDate;
    private Integer universityGrade;
    private String nationalGrade;
    private String ectsGrade;
    private CourseWorkDetailsDTO courseWorkDetails;
    private PracticeDetailsDTO practiceDetails;
    private QualificationDetailsDTO qualificationDetails;
}
