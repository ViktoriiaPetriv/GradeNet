package org.bachelor.orgservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bachelor.orgservice.model.entity.OrgType;

public record OrganizationRequestDTO (

    @NotBlank(message = "Name cannot be blank")
    String name,

    @NotNull(message = "Organization type is required")
    OrgType orgType,

    Long parentId
) {}
