package org.bachelor.gradeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.config.UserServiceClient;
import org.bachelor.gradeservice.exception.NotFoundException;
import org.bachelor.gradeservice.exception.RestException;
import org.bachelor.gradeservice.mapper.GradeBookEntryMapper;
import org.bachelor.gradeservice.mapper.GradeMapper;
import org.bachelor.gradeservice.mapper.HoursMapper;
import org.bachelor.gradeservice.model.dto.*;
import org.bachelor.gradeservice.model.entity.*;
import org.bachelor.gradeservice.repository.GradeBookEntryRepository;
import org.bachelor.gradeservice.repository.SpecialtyDisciplineRepository;
import org.bachelor.gradeservice.service.GradeBookEntryService;
import org.bachelor.gradeservice.utils.GradeConverter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.bachelor.gradeservice.repository.GradeBookEntrySpecification.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeBookEntryServiceImpl implements GradeBookEntryService {

    private final GradeBookEntryRepository entryRepository;
    private final SpecialtyDisciplineRepository specialtyDisciplineRepository;
    private final GradeBookEntryMapper entryMapper;
    private final GradeMapper gradeMapper;
    private final HoursMapper hoursMapper;
    private final GradeConverter gradeConverter;
    private final UserServiceClient userServiceClient;

    @Transactional
    @Override
    public List<GradeBookEntryDTO> create(GradeBookEntryCreateDTO dto) {
        SpecialtyDiscipline sd = specialtyDisciplineRepository.findById(dto.getSpecialtyDisciplineId())
                .orElseThrow(() -> new NotFoundException("Прив'язку дисципліни не знайдено"));

        Long disciplineId = sd.getDiscipline().getId();

        return dto.getBookNumberIds().stream()
                .map(bookNumberId -> {
                    if (entryRepository.existsByBookNumberIdAndSpecialtyDiscipline_Discipline_IdAndResult(
                            bookNumberId, disciplineId, EntryResult.PASSED)) {
                        throw new RestException("Студент " + bookNumberId +
                                " вже успішно пройшов цю дисципліну");
                    }

                    if (entryRepository.existsByBookNumberIdAndSpecialtyDiscipline_Discipline_IdAndStatus(
                            bookNumberId, disciplineId, EntryStatus.IN_PROGRESS)) {
                        throw new RestException("Студент " + bookNumberId +
                                " вже проходить цю дисципліну на іншій спеціальності");
                    }

                    entryRepository.findTopByBookNumberIdAndSpecialtyDisciplineIdOrderByAttemptDesc(
                                    bookNumberId, dto.getSpecialtyDisciplineId())
                            .ifPresent(existing -> {
                                if (existing.getStatus() == EntryStatus.IN_PROGRESS) {
                                    throw new RestException("Студент " + bookNumberId + " вже має відкритий запис");
                                }
                                if (existing.getResult() == EntryResult.PASSED) {
                                    throw new RestException("Студент " + bookNumberId + " вже зарахований");
                                }
                            });

                    int nextAttempt = nextAttempt(bookNumberId, dto.getSpecialtyDisciplineId());
                    if (dto.getMinAttempt() != null && dto.getMinAttempt() > nextAttempt) {
                        nextAttempt = dto.getMinAttempt();
                    }

                    GradeBookEntry entry = new GradeBookEntry();
                    entry.setBookNumberId(bookNumberId);
                    entry.setSpecialtyDiscipline(sd);
                    entry.setProfessorId(dto.getProfessorId());
                    entry.setAcademicYear(dto.getAcademicYear());
                    entry.setAttempt(nextAttempt);
                    entry.setStatus(EntryStatus.IN_PROGRESS);
                    entry.setReportDate(dto.getReportDate());
                    entry.setSemester(dto.getSemester());
                    return entryMapper.toDTO(entryRepository.save(entry));
                })
                .toList();
    }

    // Повторна спроба — закриває попередній запис як FAILED і створює новий
    @Transactional
    @Override
    public GradeBookEntryDTO retake(Long entryId, Long professorId) {
        GradeBookEntry previous = entryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Запис не знайдено"));

        if (previous.getStatus() == EntryStatus.IN_PROGRESS) {
            throw new RestException("Спочатку закрийте поточний запис");
        }

        int nextAttempt = nextAttempt(
                previous.getBookNumberId(),
                previous.getSpecialtyDiscipline().getId());

        GradeBookEntry retake = new GradeBookEntry();
        retake.setBookNumberId(previous.getBookNumberId());
        retake.setSpecialtyDiscipline(previous.getSpecialtyDiscipline());
        retake.setProfessorId(professorId);          // новий викладач
        retake.setAcademicYear(previous.getAcademicYear());
        retake.setAttempt(nextAttempt);
        retake.setStatus(EntryStatus.IN_PROGRESS);
        entryRepository.save(retake);

        return entryMapper.toDTO(retake);
    }

    // Закрити запис із фінальним результатом
    // service/impl/GradeBookEntryServiceImpl.java — метод closeAll
    @Transactional
    @Override
    public CloseEntryResponse closeAll(CloseEntryDTO closeEntryDTO) {
        List<GradeBookEntryDTO> closed = new ArrayList<>();
        List<CloseEntryError> errors = new ArrayList<>();

        for (Long entryId : closeEntryDTO.getEntryIds()) {
            entryRepository.findById(entryId).ifPresentOrElse(entry -> {
                if (entry.getStatus() == EntryStatus.COMPLETED) {
                    errors.add(new CloseEntryError(entryId, "Запис вже закрито"));
                    return;
                }
                if (entry.getGrades().isEmpty()) {
                    errors.add(new CloseEntryError(entryId, "Неможливо закрити запис без оцінки"));
                    return;
                }

                Grade lastGrade = entry.getGrades().getLast();
                EntryResult result = gradeConverter.toEntryResult(lastGrade.getUniversityGrade());

                entry.setStatus(EntryStatus.COMPLETED);
                entry.setResult(result);
                closed.add(entryMapper.toDTO(entry));
            }, () -> errors.add(new CloseEntryError(entryId, "Запис не знайдено")));
        }

        return new CloseEntryResponse(closed, errors);
    }

    @Override
    public GradeBookEntryDTO getById(Long id) {
        return entryRepository.findById(id)
                .map(entryMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Запис не знайдено"));
    }

    @Override
    public PageResponse<GradeBookEntryDTO> getAll(GradeBookEntryFilter filter, Pageable pageable) {
        Specification<GradeBookEntry> spec = Specification.allOf(
                byBookNumberId(filter.getBookNumberId()),
                bySpecialtyDisciplineId(filter.getSpecialtyDisciplineId()),
                byProfessorId(filter.getProfessorId()),
                byAcademicYear(filter.getAcademicYear()),
                byStatus(filter.getStatus()),
                byResult(filter.getResult()),
                bySemester(filter.getSemester())
        );
        return PageResponse.of(
                entryRepository.findAll(spec, pageable).map(entry -> {
                    GradeBookEntryDTO dto = entryMapper.toDTO(entry);
                    dto.setStudentName(userServiceClient.getStudentName(entry.getBookNumberId()));
                    return dto;
                })
        );
    }

    @Transactional
    @Override
    public void delete(Long id) {
        if (!entryRepository.existsById(id)) {
            throw new NotFoundException("Запис не знайдено");
        }
        entryRepository.deleteById(id);
    }

    @Override
    public List<StudentDisciplineDTO> getStudentDisciplines(Long bookNumberId,
                                                            StudentDisciplineFilter filter) {
        Specification<GradeBookEntry> spec = Specification.allOf(
                byBookNumberId(bookNumberId),
                byAcademicYears(filter.getAcademicYears())
        );

        return entryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "academicYear"))
                .stream()
                .map(entry -> {
                    Set<HoursDTO> filteredHours = entry.getSpecialtyDiscipline().getHours()
                            .stream()
                            .filter(h -> h.getAcademicYear().equals(entry.getAcademicYear()))
                            .map(hoursMapper::toDTO)
                            .collect(Collectors.toSet());
                    StudentDisciplineDTO dto = entryMapper.toStudentDisciplineDTO(entry, filteredHours);
                    dto.setProfessorName(userServiceClient.getProfessorName(entry.getProfessorId()));
                    dto.setReportDate(entry.getReportDate());
                    return dto;
                })
                .toList();
    }

    @Override
    public List<BulkGradeEntryDTO> getBulkEntries(Long specialtyDisciplineId, String academicYear) {
        Specification<GradeBookEntry> spec = Specification.allOf(
                bySpecialtyDisciplineId(specialtyDisciplineId),
                byAcademicYear(academicYear),
                byStatus(EntryStatus.IN_PROGRESS)
        );
        return entryRepository.findAll(spec).stream()
                .map(this::toBulkGradeEntryDTO)
                .toList();
    }

    @Override
    public List<BulkGradeEntryDTO> getGroupReport(Long specialtyDisciplineId, String academicYear) {
        Specification<GradeBookEntry> spec = Specification.allOf(
                bySpecialtyDisciplineId(specialtyDisciplineId),
                byAcademicYear(academicYear)
        );
        return entryRepository.findAll(spec).stream()
                .map(this::toBulkGradeEntryDTO)
                .toList();
    }

    private BulkGradeEntryDTO toBulkGradeEntryDTO(GradeBookEntry entry) {
        BulkGradeEntryDTO dto = new BulkGradeEntryDTO();
        dto.setEntryId(entry.getId());
        dto.setBookNumberId(entry.getBookNumberId());
        dto.setStudentName(userServiceClient.getStudentName(entry.getBookNumberId()));
        dto.setReportDate(entry.getReportDate());
        dto.setStatus(entry.getStatus());
        dto.setResult(entry.getResult());
        if (!entry.getGrades().isEmpty()) {
            dto.setLatestGrade(gradeMapper.toDTO(entry.getGrades().getLast()));
        }
        return dto;
    }

    private int nextAttempt(Long bookNumberId, Long specialtyDisciplineId) {
        return entryRepository
                .findTopByBookNumberIdAndSpecialtyDisciplineIdOrderByAttemptDesc(
                        bookNumberId, specialtyDisciplineId)
                .map(e -> e.getAttempt() + 1)
                .orElse(1);
    }
}
