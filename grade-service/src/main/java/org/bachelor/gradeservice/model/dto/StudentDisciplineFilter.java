package org.bachelor.gradeservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StudentDisciplineFilter {
    private List<String> academicYears;
}
