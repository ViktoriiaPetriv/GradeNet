package org.bachelor.gradeservice.service;

import org.bachelor.gradeservice.model.dto.GradeDTO;
import org.bachelor.gradeservice.model.dto.GradeRequestDTO;

import java.util.List;

public interface GradeService {
    GradeDTO create(GradeRequestDTO dto);
    GradeDTO update(Long id, GradeRequestDTO dto);
    GradeDTO getById(Long id);
    List<GradeDTO> getAllBySpecialtyDiscipline(Long specialtyDisciplineId);
    List<GradeDTO> getAllByStudent(Long studentId);
    void delete(Long id);
}
