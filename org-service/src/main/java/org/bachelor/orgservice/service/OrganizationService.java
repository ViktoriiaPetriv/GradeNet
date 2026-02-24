package org.bachelor.orgservice.service;

import org.bachelor.orgservice.model.dto.OrganizationDTO;
import org.bachelor.orgservice.model.dto.OrganizationRequestDTO;
import org.bachelor.orgservice.model.dto.OrganizationShortDTO;
import org.bachelor.orgservice.model.dto.PageResponse;
import org.bachelor.orgservice.model.entity.OrgType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrganizationService {
    OrganizationDTO create(OrganizationRequestDTO dto);
    OrganizationDTO update(Long id, OrganizationRequestDTO dto);
    OrganizationDTO getById(Long id);
    void delete(Long id);
    PageResponse<OrganizationDTO> getAll(OrgType orgType, Pageable pageable);
    List<OrganizationShortDTO> getAllShort(OrgType orgType);
}
