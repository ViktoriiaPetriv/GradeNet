package org.bachelor.gradeservice.model.dto;

import lombok.Data;

@Data
public class HoursDTO {
    private Long id;
    private String academicYear;
    private Integer ectsCredits;
    private Integer totalHours;
    private Integer classroomHours;
    private Integer lectureHours;
    private Integer seminarHours;
    private Integer laboratoryHours;
    private Integer individualHours;
    private Integer selfWorkHours;
}
