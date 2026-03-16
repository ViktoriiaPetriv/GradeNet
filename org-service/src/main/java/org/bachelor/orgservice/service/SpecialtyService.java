package org.bachelor.orgservice.service;

import org.bachelor.orgservice.model.dto.OrgInfoDTO;
import org.bachelor.orgservice.model.dto.PageResponse;
import org.bachelor.orgservice.model.dto.SpecialtyDTO;
import org.bachelor.orgservice.model.dto.SpecialtyRequestDTO;
import org.bachelor.orgservice.model.entity.Degree;
import org.bachelor.orgservice.model.entity.EduType;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SpecialtyService {
    SpecialtyDTO create(SpecialtyRequestDTO dto);

    SpecialtyDTO update(Long id, SpecialtyRequestDTO dto);

    SpecialtyDTO getById(Long id);

    PageResponse<SpecialtyDTO> getAll(Degree degree, EduType eduType, Pageable pageable);

    PageResponse<SpecialtyDTO> getAllByOrganization(Long orgId, Degree degree, EduType eduType, Pageable pageable);

    void delete(Long id);

    OrgInfoDTO getOrgInfo(Long specialtyId);

    List<Long> getIdsByOrgIds(List<Long> orgIds);
}
