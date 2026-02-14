package org.bachelor.orgservice.model.dto;

import lombok.Data;
import org.bachelor.orgservice.model.entity.OrgType;

@Data
public class OrganizationDTO {
    private Long id;
    private String name;
    private OrgType orgType;
    private Long parentId;
}
