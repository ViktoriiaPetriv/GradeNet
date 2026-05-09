package org.bachelor.gradeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.exception.NotFoundException;
import org.bachelor.gradeservice.exception.RestException;
import org.bachelor.gradeservice.mapper.HoursMapper;
import org.bachelor.gradeservice.model.dto.HoursCreateDTO;
import org.bachelor.gradeservice.model.dto.HoursDTO;
import org.bachelor.gradeservice.model.entity.Hours;
import org.bachelor.gradeservice.model.entity.HoursTemplate;
import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.bachelor.gradeservice.repository.HoursRepository;
import org.bachelor.gradeservice.repository.HoursTemplateRepository;
import org.bachelor.gradeservice.repository.SpecialtyDisciplineRepository;
import org.bachelor.gradeservice.service.HoursService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class HoursServiceImpl implements HoursService {
    private final SpecialtyDisciplineRepository specialtyDisciplineRepository;
    private final HoursRepository hoursRepository;
    private final HoursTemplateRepository hoursTemplateRepository;
    private final HoursMapper hoursMapper;

    @Transactional
    @Override
    public HoursDTO addHours(Long specialtyDisciplineId, HoursCreateDTO dto) {
        SpecialtyDiscipline sd = specialtyDisciplineRepository.findById(specialtyDisciplineId)
                .orElseThrow(() -> new NotFoundException("Прив'язку дисципліни до спеціальності не знайдено"));

        if (hoursRepository.existsBySpecialtyDisciplineIdAndAcademicYear(
                specialtyDisciplineId, dto.getAcademicYear())) {
            throw new RestException("Години для цього навчального року вже існують");
        }

        HoursTemplate template = findOrCreateTemplate(dto);

        Hours hours = new Hours();
        hours.setSpecialtyDiscipline(sd);
        hours.setAcademicYear(dto.getAcademicYear());
        hours.setTemplate(template);
        hoursRepository.save(hours);

        return hoursMapper.toDTO(hours);
    }

    @Transactional
    @Override
    public HoursDTO updateHours(Long hoursId, HoursCreateDTO dto) {
        Hours hours = hoursRepository.findById(hoursId)
                .orElseThrow(() -> new NotFoundException("Години не знайдено"));

        boolean yearChanged = !hours.getAcademicYear().equals(dto.getAcademicYear());
        if (yearChanged && hoursRepository.existsBySpecialtyDisciplineIdAndAcademicYear(
                hours.getSpecialtyDiscipline().getId(), dto.getAcademicYear())) {
            throw new RestException("Години для цього навчального року вже існують");
        }

        HoursTemplate template = findOrCreateTemplate(dto);

        hours.setAcademicYear(dto.getAcademicYear());
        hours.setTemplate(template);

        return hoursMapper.toDTO(hours);
    }

    // Знаходить існуючий шаблон або створює новий (дедуплікація)
    private HoursTemplate findOrCreateTemplate(HoursCreateDTO dto) {
        return hoursTemplateRepository
                .findByEctsCreditsAndTotalHoursAndClassroomHoursAndLectureHoursAndSeminarHoursAndLaboratoryHoursAndIndividualHoursAndSelfWorkHours(
                        dto.getEctsCredits(), dto.getTotalHours(), dto.getClassroomHours(),
                        dto.getLectureHours(), dto.getSeminarHours(), dto.getLaboratoryHours(),
                        dto.getIndividualHours(), dto.getSelfWorkHours()
                )
                .orElseGet(() -> hoursTemplateRepository.save(hoursMapper.toTemplate(dto)));
    }

    @Transactional
    @Override
    public void deleteHours(Long hoursId) {
        if (!hoursRepository.existsById(hoursId)) {
            throw new NotFoundException("Години не знайдено");
        }
        hoursRepository.deleteById(hoursId);
    }

    @Override
    public HoursDTO getById(Long id) {
        return hoursRepository.findById(id)
                .map(hoursMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Години не знайдено"));
    }

    @Override
    public List<HoursDTO> getAll(Long specialtyDisciplineId) {
        return hoursRepository.findAllBySpecialtyDisciplineId(specialtyDisciplineId)
                .stream()
                .map(hoursMapper::toDTO)
                .toList();
    }
}
