package org.bachelor.integrationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisciplineCheckResultDTO {
    private String groupName;
    private String academicYear;
    private String specialtyName;
    private Long specialtyId;
    private Integer graduationYear;
    private Long specialtyOfferingId;
    private List<DisciplineCheckItemDTO> disciplines;
}
