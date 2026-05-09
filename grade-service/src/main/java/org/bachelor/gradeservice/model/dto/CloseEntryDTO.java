package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.bachelor.gradeservice.model.entity.EntryResult;

import java.util.List;

@Data
public class CloseEntryDTO {
    @NotEmpty
    private List<Long> entryIds;
}
