package org.bachelor.gradeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.model.dto.AuthenticatedUser;
import org.bachelor.gradeservice.utils.AcademicCalendar;
import org.bachelor.gradeservice.utils.GradeConverter;
import org.bachelor.gradeservice.exception.NotFoundException;
import org.bachelor.gradeservice.mapper.GradeMapper;
import org.bachelor.gradeservice.model.dto.GradeDTO;
import org.bachelor.gradeservice.model.dto.GradeRequestDTO;
import org.bachelor.gradeservice.model.entity.Grade;
import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.bachelor.gradeservice.repository.GradeRepository;
import org.bachelor.gradeservice.repository.SpecialtyDisciplineRepository;
import org.bachelor.gradeservice.service.GradeService;
import org.bachelor.gradeservice.utils.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final SpecialtyDisciplineRepository specialtyDisciplineRepository;
    private final GradeMapper gradeMapper;
    private final GradeConverter gradeConverter;
    private final AcademicCalendar academicCalendar;

    @Override
    @Transactional
    public GradeDTO create(GradeRequestDTO dto) {
        SpecialtyDiscipline sd = getSpecialtyDisciplineOrThrow(dto.specialtyDisciplineId());

        validateReportDateAccess(sd);

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        String academicYear;
        int semester;

        if (currentUser.isProfessor()) {
            academicYear = academicCalendar.getCurrentAcademicYear();
            semester = academicCalendar.getCurrentSemester();
        } else {
            academicYear = dto.academicYear() != null
                    ? dto.academicYear()
                    : academicCalendar.getCurrentAcademicYear();
            semester = dto.semester() != null
                    ? dto.semester()
                    : academicCalendar.getCurrentSemester();
        }

        int attempt = gradeRepository
                .findMaxAttemptBySpecialtyDisciplineIdAndStudentId(
                        dto.specialtyDisciplineId(), dto.studentId())
                .map(max -> max + 1)
                .orElse(1);

        Grade grade = gradeMapper.toEntity(dto);
        grade.setSpecialtyDiscipline(sd);
        grade.setAttempt(attempt);
        grade.setAcademicYear(academicYear);
        grade.setSemester(semester);
        convertGrades(grade, dto.universityGrade());

        return gradeMapper.toDto(gradeRepository.save(grade));
    }

    @Override
    @Transactional
    public GradeDTO update(Long id, GradeRequestDTO dto) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Оцінку не знайдено"));

        validateReportDateAccess(grade.getSpecialtyDiscipline());

        grade.setAssessmentDate(dto.assessmentDate());
        grade.setAssessment(dto.assessment());
        grade.setState(dto.state());
        convertGrades(grade, dto.universityGrade());

        return gradeMapper.toDto(gradeRepository.save(grade));
    }

    @Override
    public GradeDTO getById(Long id) {
        return gradeRepository.findById(id)
                .map(gradeMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Оцінку не знайдено"));
    }

    @Override
    public List<GradeDTO> getAllBySpecialtyDiscipline(Long specialtyDisciplineId) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.isProfessor()) {
            SpecialtyDiscipline sd = getSpecialtyDisciplineOrThrow(specialtyDisciplineId);
            if (!currentUser.getUserId().equals(sd.getProfessorId())) {
                throw new AccessDeniedException("Ви не є викладачем цієї дисципліни");
            }
        }

        return gradeRepository.findAllBySpecialtyDisciplineId(specialtyDisciplineId)
                .stream().map(gradeMapper::toDto).toList();
    }

    @Override
    public List<GradeDTO> getAllByStudent(Long studentId) {
        return gradeRepository.findAllByStudentId(studentId)
                .stream().map(gradeMapper::toDto).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Оцінку не знайдено"));

        validateReportDateAccess(grade.getSpecialtyDiscipline());

        gradeRepository.delete(grade);
    }

    private SpecialtyDiscipline getSpecialtyDisciplineOrThrow(Long id) {
        return specialtyDisciplineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Дисципліну не знайдено"));
    }

    private void convertGrades(Grade grade, Integer universityGrade) {
        grade.setUniversityGrade(universityGrade);
        grade.setNationalGrade(gradeConverter.toNational(universityGrade, grade.getAssessment()));
        grade.setEctsGrade(gradeConverter.toEcts(universityGrade));
    }

    private void validateReportDateAccess(SpecialtyDiscipline sd) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.isAdmin() || currentUser.isManager()) return;

        if (sd.getReportDate() == null) return;

        if (Instant.now().isAfter(sd.getReportDate())) {
            throw new AccessDeniedException(
                    "Термін виставлення оцінок закінчився %s"
                            .formatted(sd.getReportDate()));
        }

        if (currentUser.isProfessor()) {
            if (!currentUser.getUserId().equals(sd.getProfessorId())) {
                throw new AccessDeniedException("Ви не є викладачем цієї дисципліни");
            }
        }
    }
}
