package org.bachelor.gradeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.config.OrgServiceClient;
import org.bachelor.gradeservice.config.UserServiceClient;
import org.bachelor.gradeservice.exception.EntityExistsException;
import org.bachelor.gradeservice.exception.NotFoundException;
import org.bachelor.gradeservice.mapper.DisciplineMapper;
import org.bachelor.gradeservice.model.dto.DisciplineDTO;
import org.bachelor.gradeservice.model.dto.DisciplineRequestDTO;
import org.bachelor.gradeservice.model.entity.Discipline;
import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.bachelor.gradeservice.repository.DisciplineRepository;
import org.bachelor.gradeservice.repository.SpecialtyDisciplineRepository;
import org.bachelor.gradeservice.service.DisciplineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisciplineServiceImpl implements DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final SpecialtyDisciplineRepository specialtyDisciplineRepository;
    private final DisciplineMapper disciplineMapper;
    private final OrgServiceClient orgServiceClient;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public DisciplineDTO create(DisciplineRequestDTO dto) {
        validateSpecialtyExists(dto.specialtyId());
        validateProfessorIfPresent(dto.professorId());

        Discipline discipline = disciplineRepository.findByName(dto.name())
                .orElseGet(() -> disciplineRepository.save(disciplineMapper.toEntity(dto)));

        if (specialtyDisciplineRepository.existsByDisciplineIdAndSpecialtyId(
                discipline.getId(), dto.specialtyId())) {
            throw new EntityExistsException("Дисципліна вже прив'язана до цієї спеціальності");
        }

        SpecialtyDiscipline sd = new SpecialtyDiscipline();
        sd.setDiscipline(discipline);
        sd.setSpecialtyId(dto.specialtyId());
        sd.setProfessorId(dto.professorId());
        sd.setReportDate(dto.reportDate());
        specialtyDisciplineRepository.save(sd);

        return disciplineMapper.toDto(discipline, sd);
    }

    @Override
    @Transactional
    public DisciplineDTO update(Long specialtyDisciplineId, DisciplineRequestDTO dto) {
        SpecialtyDiscipline sd = getSpecialtyDisciplineOrThrow(specialtyDisciplineId);
        Discipline discipline = sd.getDiscipline();

        if (!discipline.getName().equals(dto.name())) {
            disciplineRepository.findByName(dto.name()).ifPresent(d -> {
                throw new EntityExistsException("Дисципліна з такою назвою вже існує");
            });
            discipline.setName(dto.name());
        }

        if (!sd.getSpecialtyId().equals(dto.specialtyId())) {
            validateSpecialtyExists(dto.specialtyId());

            if (specialtyDisciplineRepository.existsByDisciplineIdAndSpecialtyId(
                    discipline.getId(), dto.specialtyId())) {
                throw new EntityExistsException("Такий зв'язок вже існує");
            }
            sd.setSpecialtyId(dto.specialtyId());
        }

        if (dto.professorId() != null && !dto.professorId().equals(sd.getProfessorId())) {
            validateProfessorIfPresent(dto.professorId());
        }

        sd.setProfessorId(dto.professorId());
        sd.setReportDate(dto.reportDate());

        return disciplineMapper.toDto(discipline, sd);
    }

    @Override
    public DisciplineDTO getById(Long specialtyDisciplineId) {
        SpecialtyDiscipline sd = getSpecialtyDisciplineOrThrow(specialtyDisciplineId);
        return disciplineMapper.toDto(sd.getDiscipline(), sd);
    }

    @Override
    public List<DisciplineDTO> getAll() {
        return specialtyDisciplineRepository.findAll().stream()
                .map(sd -> disciplineMapper.toDto(sd.getDiscipline(), sd))
                .toList();
    }

    @Override
    public List<DisciplineDTO> getAllBySpecialty(Long specialtyId) {
        return specialtyDisciplineRepository.findAllBySpecialtyId(specialtyId).stream()
                .map(sd -> disciplineMapper.toDto(sd.getDiscipline(), sd))
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long specialtyDisciplineId) {
        SpecialtyDiscipline sd = getSpecialtyDisciplineOrThrow(specialtyDisciplineId);
        specialtyDisciplineRepository.delete(sd);
    }

    private void validateSpecialtyExists(Long specialtyId) {
        if (!orgServiceClient.specialtyExists(specialtyId)) {
            throw new NotFoundException("Спеціальність з ID %s не знайдено".formatted(specialtyId));
        }
    }

    private void validateProfessorIfPresent(Long professorId) {
        if (professorId == null) return;
        if (!userServiceClient.professorExists(professorId)) {
            throw new NotFoundException(
                    "Викладача з ID %s не знайдено або користувач не має ролі PROFESSOR"
                            .formatted(professorId));
        }
    }

    private SpecialtyDiscipline getSpecialtyDisciplineOrThrow(Long id) {
        return specialtyDisciplineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Запис не знайдено"));
    }
}
