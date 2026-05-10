package org.bachelor.integrationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ParsedStudentRow {
    private String fullName;
    private List<ParsedGrade> grades;
}
