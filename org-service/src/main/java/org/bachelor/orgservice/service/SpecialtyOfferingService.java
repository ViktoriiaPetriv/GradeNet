package org.bachelor.orgservice.service;

import org.bachelor.orgservice.model.dto.SpecialtyOfferingDTO;
import org.bachelor.orgservice.model.dto.SpecialtyOfferingRequestDTO;

import java.util.List;

public interface SpecialtyOfferingService {
    SpecialtyOfferingDTO create(SpecialtyOfferingRequestDTO request);
    SpecialtyOfferingDTO getById(Long id);
    List<SpecialtyOfferingDTO> getAllBySpecialty(Long specialtyId);
    List<Long> getIdsBySpecialtyIds(List<Long> specialtyIds);
    void delete(Long id);
}
