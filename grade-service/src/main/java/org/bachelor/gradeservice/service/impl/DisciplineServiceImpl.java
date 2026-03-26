package org.bachelor.gradeservice.service.impl;

import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.exception.NotFoundException;
import org.bachelor.gradeservice.mapper.DisciplineMapper;
import org.bachelor.gradeservice.model.dto.DisciplineDTO;
import org.bachelor.gradeservice.model.dto.DisciplineRequestDTO;
import org.bachelor.gradeservice.model.entity.Discipline;
import org.bachelor.gradeservice.repository.DisciplineRepository;
import org.bachelor.gradeservice.service.DisciplineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisciplineServiceImpl implements DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final DisciplineMapper disciplineMapper;

    @Override
    @Transactional
    public DisciplineDTO create(DisciplineRequestDTO dto) {
        if (disciplineRepository.findByName(dto.name()).isPresent()) {
            throw new EntityExistsException("Дисципліна з такою назвою вже існує");
        }
        return disciplineMapper.toDto(disciplineRepository.save(disciplineMapper.toEntity(dto)));
    }

    @Override
    @Transactional
    public DisciplineDTO update(Long id, DisciplineRequestDTO dto) {
        Discipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Дисципліну не знайдено"));

        if (!discipline.getName().equals(dto.name())
                && disciplineRepository.findByName(dto.name()).isPresent()) {
            throw new EntityExistsException("Дисципліна з такою назвою вже існує");
        }

        discipline.setName(dto.name());
        return disciplineMapper.toDto(disciplineRepository.save(discipline));
    }

    @Override
    public DisciplineDTO getById(Long id) {
        return disciplineRepository.findById(id)
                .map(disciplineMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Дисципліну не знайдено"));
    }

    @Override
    public List<DisciplineDTO> getAll() {
        return disciplineRepository.findAll().stream()
                .map(disciplineMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Discipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Дисципліну не знайдено"));
        disciplineRepository.delete(discipline);
    }
}
