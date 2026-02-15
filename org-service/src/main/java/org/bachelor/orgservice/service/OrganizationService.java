package org.bachelor.orgservice.service;

import org.bachelor.orgservice.model.dto.OrganizationDTO;
import org.bachelor.orgservice.model.dto.OrganizationRequestDTO;

import java.util.List;

public interface OrganizationService {
    OrganizationDTO create(OrganizationRequestDTO dto);
    OrganizationDTO update(Long id, OrganizationRequestDTO dto);
    OrganizationDTO getById(Long id);
    void delete(Long id);
    List<OrganizationDTO> getAll();
}
