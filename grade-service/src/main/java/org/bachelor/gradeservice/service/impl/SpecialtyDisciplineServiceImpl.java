package org.bachelor.gradeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.config.OrgServiceClient;
import org.bachelor.gradeservice.exception.NotFoundException;
import org.bachelor.gradeservice.exception.RestException;
import org.bachelor.gradeservice.mapper.SpecialtyDisciplineMapper;
import org.bachelor.gradeservice.model.dto.SpecialtyDisciplineDTO;
import org.bachelor.gradeservice.model.dto.SpecialtyDisciplineFilter;
import org.bachelor.gradeservice.model.entity.*;
import org.bachelor.gradeservice.repository.DisciplineRepository;
import org.bachelor.gradeservice.repository.SpecialtyDisciplineRepository;
import org.bachelor.gradeservice.service.SpecialtyDisciplineService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.bachelor.gradeservice.repository.SpecialtyDisciplineSpecification.byDisciplineId;
import static org.bachelor.gradeservice.repository.SpecialtyDisciplineSpecification.bySpecialtyId;

@RequiredArgsConstructor
@Service
public class SpecialtyDisciplineServiceImpl implements SpecialtyDisciplineService {
    private final DisciplineRepository disciplineRepository;
    private final SpecialtyDisciplineRepository specialtyDisciplineRepository;
    private final SpecialtyDisciplineMapper specialtyDisciplineMapper;
    private final OrgServiceClient orgServiceClient;

    @Transactional
    @Override
    public SpecialtyDisciplineDTO addSpecialty(Long disciplineId, Long specialtyId) {
        if (!orgServiceClient.specialtyExists(specialtyId)) {
            throw new NotFoundException("Спеціальність не знайдено");
        }

        Discipline discipline = disciplineRepository.findById(disciplineId)
                .orElseThrow(() -> new NotFoundException("Дисципліну не знайдено"));

        if (specialtyDisciplineRepository.existsBySpecialtyIdAndDisciplineId(specialtyId, disciplineId)) {
            throw new RestException("Дисципліна вже прив'язана до цієї спеціальності");
        }

        SpecialtyDiscipline sd = new SpecialtyDiscipline();
        sd.setSpecialtyId(specialtyId);
        sd.setDiscipline(discipline);
        specialtyDisciplineRepository.save(sd);

        return specialtyDisciplineMapper.toDTO(sd);
    }

    @Transactional
    @Override
    public void removeSpecialty(Long disciplineId, Long specialtyId) {
        SpecialtyDiscipline sd = specialtyDisciplineRepository
                .findBySpecialtyIdAndDisciplineId(specialtyId, disciplineId)
                .orElseThrow(() -> new NotFoundException("Прив'язку не знайдено"));

        specialtyDisciplineRepository.delete(sd);
    }

    @Override
    public SpecialtyDisciplineDTO getById(Long id) {
        return specialtyDisciplineRepository.findById(id)
                .map(specialtyDisciplineMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Прив'язку не знайдено"));
    }

    @Override
    public List<SpecialtyDisciplineDTO> getAll(SpecialtyDisciplineFilter filter) {
        Specification<SpecialtyDiscipline> spec = Specification.allOf(
                byDisciplineId(filter.getDisciplineId()),
                bySpecialtyId(filter.getSpecialtyId())
        );

        return specialtyDisciplineRepository.findAll(spec)
                .stream()
                .map(specialtyDisciplineMapper::toDTO)
                .toList();
    }
}
