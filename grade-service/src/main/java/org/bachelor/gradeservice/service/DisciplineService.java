package org.bachelor.gradeservice.service;

import org.bachelor.gradeservice.model.dto.*;
import org.bachelor.gradeservice.model.dto.DisciplineCreateResponseDTO;

import java.util.List;

public interface DisciplineService {
    DisciplineCreateResponseDTO create(DisciplineCreateDTO dto);
    List<DisciplineDTO> getAll();
    DisciplineDTO getById(Long id);
    DisciplineDTO update(Long id, DisciplineUpdateDTO dto);
    void delete(Long id);
}
