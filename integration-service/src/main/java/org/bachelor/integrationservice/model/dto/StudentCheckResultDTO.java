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
public class StudentCheckResultDTO {
    private String groupName;
    private List<StudentCheckItemDTO> students;
}
