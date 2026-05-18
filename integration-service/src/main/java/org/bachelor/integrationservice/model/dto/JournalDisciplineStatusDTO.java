package org.bachelor.integrationservice.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class JournalDisciplineStatusDTO {
    private Long externalId;
    private String name;
    private Integer semester;
    private Integer totalHours;
    private String academicYear;
    private boolean existsInSystem;
    private List<Integer> attempts;
}
