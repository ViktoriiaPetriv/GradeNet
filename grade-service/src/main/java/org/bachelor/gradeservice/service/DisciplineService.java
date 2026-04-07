package org.bachelor.gradeservice.service;

import org.bachelor.gradeservice.model.dto.DisciplineDTO;
import org.bachelor.gradeservice.model.dto.DisciplineRequestDTO;

import java.util.List;

public interface DisciplineService {
    DisciplineDTO create(DisciplineRequestDTO dto);
    DisciplineDTO update(Long id, DisciplineRequestDTO dto);
    DisciplineDTO getById(Long id);
    List<DisciplineDTO> getAll();
    List<DisciplineDTO> getAllBySpecialty(Long specialtyId);
    void delete(Long id);
}
