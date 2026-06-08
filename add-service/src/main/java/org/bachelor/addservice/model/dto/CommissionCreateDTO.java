package org.bachelor.addservice.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CommissionCreateDTO {

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;
}
