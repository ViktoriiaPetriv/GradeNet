package org.bachelor.gradeservice.service;

import org.bachelor.gradeservice.model.dto.SpecialtyDisciplineDTO;
import org.bachelor.gradeservice.model.dto.SpecialtyDisciplineFilter;

import java.util.List;

public interface SpecialtyDisciplineService {
    SpecialtyDisciplineDTO addSpecialty(Long disciplineId, Long specialtyId);
    void removeSpecialty(Long disciplineId, Long specialtyId);
    SpecialtyDisciplineDTO getById(Long id);
    List<SpecialtyDisciplineDTO> getAll(SpecialtyDisciplineFilter filter);
}
