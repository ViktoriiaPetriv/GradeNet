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
public class StudentCheckItemDTO {
    private String fullName;
    private Long bookNumberId;
    private Long studentId;
    private boolean existsInSystem;
    private List<GradeDataDTO> grades;
}
