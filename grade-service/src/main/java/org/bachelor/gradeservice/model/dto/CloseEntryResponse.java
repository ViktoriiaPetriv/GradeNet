package org.bachelor.gradeservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CloseEntryResponse {
    private List<GradeBookEntryDTO> closed;
    private List<CloseEntryError> errors;
}
