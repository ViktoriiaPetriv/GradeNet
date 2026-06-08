package org.bachelor.addservice.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommissionMemberCreateDTO {

    @NotNull
    private Long professorId;

    private Boolean isHead = false;
}
