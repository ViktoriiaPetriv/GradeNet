package org.bachelor.gradeservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CloseEntryError {
    private Long entryId;
    private String reason;
}
