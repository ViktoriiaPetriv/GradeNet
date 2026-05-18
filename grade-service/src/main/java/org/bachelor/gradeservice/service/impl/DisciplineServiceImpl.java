package org.bachelor.gradeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.exception.NotFoundException;
import org.bachelor.gradeservice.exception.RestException;
import org.bachelor.gradeservice.mapper.*;
import org.bachelor.gradeservice.model.dto.*;
import org.bachelor.gradeservice.model.entity.*;
import org.bachelor.gradeservice.repository.*;
import org.bachelor.gradeservice.service.DisciplineService;
import org.bachelor.gradeservice.service.HoursService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisciplineServiceImpl implements DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final SpecialtyDisciplineRepository specialtyDisciplineRepository;
    private final DisciplineMapper disciplineMapper;
    private final HoursService hoursService;

    @Transactional
    @Override
    public DisciplineCreateResponseDTO create(DisciplineCreateDTO dto) {
        if (disciplineRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new RestException("Дисципліна з такою назвою вже існує");
        }

        Discipline discipline = disciplineMapper.toEntity(dto);
        disciplineRepository.save(discipline);

        SpecialtyDiscipline sd = new SpecialtyDiscipline();
        sd.setSpecialtyOfferingId(dto.getSpecialtyOfferingId());
        sd.setDiscipline(discipline);
        specialtyDisciplineRepository.save(sd);

        // Делегуємо в HoursService — він сам знайде або створить template
        HoursDTO hoursDTO = hoursService.addHours(sd.getId(), dto.getHours());

        return disciplineMapper.toCreateResponseDTO(discipline, sd, hoursDTO);
    }

    @Override
    public List<DisciplineDTO> getAll() {
        return disciplineRepository.findAll()
                .stream()
                .map(disciplineMapper::toDTO)
                .toList();
    }

    @Override
    public DisciplineDTO getById(Long id) {
        return disciplineRepository.findById(id)
                .map(disciplineMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Дисципліну не знайдено"));
    }

    @Transactional
    @Override
    public DisciplineDTO update(Long id, DisciplineUpdateDTO dto) {
        Discipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Дисципліну не знайдено"));

        if (!discipline.getName().equalsIgnoreCase(dto.getName()) &&
                disciplineRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new RestException("Дисципліна з такою назвою вже існує");
        }

        discipline.setName(dto.getName());
        disciplineRepository.save(discipline);

        return disciplineMapper.toDTO(discipline);
    }
}
