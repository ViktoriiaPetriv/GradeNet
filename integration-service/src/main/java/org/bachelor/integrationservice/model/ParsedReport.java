package org.bachelor.integrationservice.model;

import lombok.Data;

import java.util.List;

@Data
public class ParsedReport {
    private final String groupName;
    private final String academicYear;
    private final Integer graduationYear;
    private final String specialtyName;
    private final List<ParsedDiscipline> disciplines;
    private final List<ParsedStudentRow> students;

    public List<String> getDisciplineNames() {
        return disciplines.stream().map(ParsedDiscipline::getName).toList();
    }
}
