package org.bachelor.orgservice.service;

import org.bachelor.orgservice.model.dto.SpecialtyOfferingDTO;
import org.bachelor.orgservice.model.dto.SpecialtyOfferingRequestDTO;

import java.util.List;
import java.util.Optional;

public interface SpecialtyOfferingService {
    SpecialtyOfferingDTO create(SpecialtyOfferingRequestDTO request);
    SpecialtyOfferingDTO getById(Long id);
    List<SpecialtyOfferingDTO> getAllBySpecialty(Long specialtyId);
    Optional<SpecialtyOfferingDTO> getByExternalId(Long externalId);
    List<Long> getIdsBySpecialtyIds(List<Long> specialtyIds);
    void delete(Long id);
}
