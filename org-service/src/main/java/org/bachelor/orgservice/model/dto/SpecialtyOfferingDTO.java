package org.bachelor.orgservice.model.dto;

import lombok.Data;

@Data
public class SpecialtyOfferingDTO {
    private Long id;
    private Long specialtyId;
    private Long externalId;
    private Integer graduationYear;
}
