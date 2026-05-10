package org.bachelor.integrationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ParsedReportMetaDTO {
    private String groupName;
    private String academicYear;
    private String specialtyName;
    private List<String> disciplineNames;
}
