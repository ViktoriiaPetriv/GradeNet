package org.bachelor.gradeservice.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentGradeReportDTO {
    private Long bookNumberId;
    private String bookNumber;
    private String studentFirstName;
    private String studentLastName;
    private String studentPatronymic;
    private List<StudentDisciplineDTO> disciplines;
}
