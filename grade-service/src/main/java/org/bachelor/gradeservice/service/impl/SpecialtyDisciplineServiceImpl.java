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
import static org.bachelor.gradeservice.repository.SpecialtyDisciplineSpecification.bySpecialtyOfferingId;

@RequiredArgsConstructor
@Service
public class SpecialtyDisciplineServiceImpl implements SpecialtyDisciplineService {
    private final DisciplineRepository disciplineRepository;
    private final SpecialtyDisciplineRepository specialtyDisciplineRepository;
    private final SpecialtyDisciplineMapper specialtyDisciplineMapper;
    private final OrgServiceClient orgServiceClient;

    @Transactional
    @Override
    public SpecialtyDisciplineDTO addSpecialty(Long disciplineId, Long specialtyOfferingId) {
        Discipline discipline = disciplineRepository.findById(disciplineId)
                .orElseThrow(() -> new NotFoundException("Дисципліну не знайдено"));

        if (specialtyDisciplineRepository.existsBySpecialtyOfferingIdAndDisciplineId(specialtyOfferingId, disciplineId)) {
            throw new RestException("Дисципліна вже прив'язана до цієї спеціальності");
        }

        SpecialtyDiscipline sd = new SpecialtyDiscipline();
        sd.setSpecialtyOfferingId(specialtyOfferingId);
        sd.setDiscipline(discipline);
        specialtyDisciplineRepository.save(sd);

        return specialtyDisciplineMapper.toDTO(sd);
    }

    @Transactional
    @Override
    public void removeSpecialty(Long disciplineId, Long specialtyOfferingId) {
        SpecialtyDiscipline sd = specialtyDisciplineRepository
                .findBySpecialtyOfferingIdAndDisciplineId(specialtyOfferingId, disciplineId)
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
    public boolean existsBySpecialtyOfferingId(Long specialtyOfferingId) {
        return specialtyDisciplineRepository.existsBySpecialtyOfferingId(specialtyOfferingId);
    }

    @Override
    public List<SpecialtyDisciplineDTO> getAll(SpecialtyDisciplineFilter filter) {
        Specification<SpecialtyDiscipline> spec = Specification.allOf(
                byDisciplineId(filter.getDisciplineId()),
                bySpecialtyOfferingId(filter.getSpecialtyOfferingId())
        );

        return specialtyDisciplineRepository.findAll(spec)
                .stream()
                .map(specialtyDisciplineMapper::toDTO)
                .toList();
    }
}
