package org.bachelor.addservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdditionalWorkCreateDTO {

    @NotNull
    private Long bookNumberId;

    @NotNull
    private Long commissionId;

    @NotBlank
    @Size(max = 20)
    private String type;

    @NotBlank
    private String title;

    private LocalDate eventDate;
    private Integer universityGrade;

    @Size(max = 20)
    private String nationalGrade;

    @Size(max = 2)
    private String ectsGrade;
}
