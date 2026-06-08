package org.bachelor.addservice.model.dto;

import lombok.Data;

@Data
public class CourseWorkDetailsDTO {
    private Long id;
    private Long additionalWorkId;
    private Integer semester;
    private String state;
    private Integer ectsCredits;
    private Integer totalHours;
}
