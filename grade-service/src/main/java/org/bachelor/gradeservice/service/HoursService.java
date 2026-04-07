package org.bachelor.gradeservice.service;

import org.bachelor.gradeservice.model.dto.HoursDTO;
import org.bachelor.gradeservice.model.dto.HoursRequestDTO;

public interface HoursService {
    HoursDTO create(HoursRequestDTO dto);
    HoursDTO update(Long id, HoursRequestDTO dto);
    HoursDTO getById(Long id);
    HoursDTO getBySpecialtyDiscipline(Long specialtyDisciplineId);
    void delete(Long id);
}
