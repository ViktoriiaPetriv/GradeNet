package org.bachelor.integrationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportErrorDTO {
    private String studentName;
    private String disciplineName;
    private String reason;
}
