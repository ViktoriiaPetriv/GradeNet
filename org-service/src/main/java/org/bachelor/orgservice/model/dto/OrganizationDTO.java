package org.bachelor.orgservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.bachelor.orgservice.model.entity.OrgType;

import java.util.List;

@Data
public class OrganizationDTO {
    private Long id;
    private String name;
    private OrgType orgType;
    private Long parentId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<OrganizationDTO> children;
}
