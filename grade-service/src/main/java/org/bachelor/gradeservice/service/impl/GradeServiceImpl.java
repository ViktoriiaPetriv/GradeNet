package org.bachelor.gradeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.exception.NotFoundException;
import org.bachelor.gradeservice.exception.RestException;
import org.bachelor.gradeservice.mapper.GradeMapper;
import org.bachelor.gradeservice.model.dto.AuthenticatedUser;
import org.bachelor.gradeservice.model.dto.BulkGradeCreateDTO;
import org.bachelor.gradeservice.model.dto.GradeCreateDTO;
import org.bachelor.gradeservice.model.dto.GradeDTO;
import org.bachelor.gradeservice.model.dto.GradeUpdateDTO;
import org.bachelor.gradeservice.model.entity.EntryStatus;
import org.bachelor.gradeservice.model.entity.Grade;
import org.bachelor.gradeservice.model.entity.GradeBookEntry;
import org.bachelor.gradeservice.repository.GradeBookEntryRepository;
import org.bachelor.gradeservice.repository.GradeRepository;
import org.bachelor.gradeservice.service.GradeService;
import org.bachelor.gradeservice.utils.GradeConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final GradeBookEntryRepository entryRepository;
    private final GradeMapper gradeMapper;
    private final GradeConverter gradeConverter;

    @Transactional
    @Override
    public GradeDTO create(GradeCreateDTO dto, AuthenticatedUser user) {
        GradeBookEntry entry = entryRepository.findById(dto.getEntryId())
                .orElseThrow(() -> new NotFoundException("Запис не знайдено"));

        if (entry.getStatus() == EntryStatus.COMPLETED) {
            throw new RestException("Запис вже закрито, додавання оцінки неможливе");
        }

        checkReportDateRestriction(entry, user);

        Grade grade = gradeMapper.toEntity(dto);
        grade.setEntry(entry);
        grade.setNationalGrade(gradeConverter.toNational(dto.getUniversityGrade(), dto.getAssessmentType()));
        grade.setEctsGrade(gradeConverter.toEcts(dto.getUniversityGrade()));
        gradeRepository.save(grade);

        return gradeMapper.toDTO(grade);
    }

    @Transactional
    @Override
    public List<GradeDTO> createBulk(BulkGradeCreateDTO bulkDto, AuthenticatedUser user) {
        return bulkDto.getGrades().stream()
                .map(dto -> create(dto, user))
                .toList();
    }

    @Transactional
    @Override
    public GradeDTO update(Long id, GradeUpdateDTO dto, AuthenticatedUser user) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Оцінку не знайдено"));

        GradeBookEntry entry = grade.getEntry();

        if (entry.getStatus() == EntryStatus.COMPLETED) {
            throw new RestException("Запис закрито, редагування оцінки неможливе");
        }

        checkReportDateRestriction(entry, user);

        grade.setAssessmentDate(dto.getAssessmentDate());
        grade.setUniversityGrade(dto.getUniversityGrade());
        grade.setAssessmentType(dto.getAssessmentType());
        grade.setNationalGrade(gradeConverter.toNational(dto.getUniversityGrade(), dto.getAssessmentType()));
        grade.setEctsGrade(gradeConverter.toEcts(dto.getUniversityGrade()));

        return gradeMapper.toDTO(grade);
    }

    @Transactional
    @Override
    public void delete(Long id, AuthenticatedUser user) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Оцінку не знайдено"));

        GradeBookEntry entry = grade.getEntry();

        if (entry.getStatus() == EntryStatus.COMPLETED) {
            throw new RestException("Запис закрито, видалення оцінки неможливе");
        }

        checkReportDateRestriction(entry, user);

        gradeRepository.delete(grade);
    }

    @Override
    public List<GradeDTO> getByEntryId(Long entryId) {
        return gradeRepository.findAllByEntryId(entryId)
                .stream()
                .map(gradeMapper::toDTO)
                .toList();
    }

    private void checkReportDateRestriction(GradeBookEntry entry, AuthenticatedUser user) {
        if (entry.getReportDate() != null
                && LocalDate.now().isAfter(entry.getReportDate())
                && !user.isAdmin()) {
            throw new RestException("Термін подання звіту минув. Зміни дозволені тільки адміністратору.");
        }
    }
}
