package org.bachelor.orgservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.bachelor.orgservice.model.entity.OrgType;

@Data
public class OrganizationRequestDTO {

    @NotBlank(message = "Name cannot be blank")
    private String name;
    private OrgType orgType;
    private Long parentId;
}
