package org.bachelor.gradeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.gradeservice.exception.EntityExistsException;
import org.bachelor.gradeservice.exception.NotFoundException;
import org.bachelor.gradeservice.mapper.HoursMapper;
import org.bachelor.gradeservice.model.dto.HoursDTO;
import org.bachelor.gradeservice.model.dto.HoursRequestDTO;
import org.bachelor.gradeservice.model.entity.Hours;
import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.bachelor.gradeservice.repository.HoursRepository;
import org.bachelor.gradeservice.repository.SpecialtyDisciplineRepository;
import org.bachelor.gradeservice.service.HoursService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HoursServiceImpl implements HoursService {

    private final HoursRepository hoursRepository;
    private final SpecialtyDisciplineRepository specialtyDisciplineRepository;
    private final HoursMapper hoursMapper;

    @Override
    @Transactional
    public HoursDTO create(HoursRequestDTO dto) {
        if (hoursRepository.existsBySpecialtyDisciplineId(dto.specialtyDisciplineId())) {
            throw new EntityExistsException("Години для цієї дисципліни вже існують");
        }

        SpecialtyDiscipline sd = getSpecialtyDisciplineOrThrow(dto.specialtyDisciplineId());

        Hours hours = hoursMapper.toEntity(dto);
        hours.setSpecialtyDiscipline(sd);

        return hoursMapper.toDto(hoursRepository.save(hours));
    }

    @Override
    @Transactional
    public HoursDTO update(Long id, HoursRequestDTO dto) {
        Hours hours = hoursRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Години не знайдено"));

        hours.setEctsHours(dto.ectsHours());
        hours.setAllHours(dto.allHours());
        hours.setTotalClassroomHours(dto.totalClassroomHours());
        hours.setLecture(dto.lecture());
        hours.setSeminar(dto.seminar());
        hours.setLaboratory(dto.laboratory());
        hours.setIndividual(dto.individual());
        hours.setSelfWork(dto.selfWork());

        return hoursMapper.toDto(hoursRepository.save(hours));
    }

    @Override
    public HoursDTO getById(Long id) {
        return hoursRepository.findById(id)
                .map(hoursMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Години не знайдено"));
    }

    @Override
    public HoursDTO getBySpecialtyDiscipline(Long specialtyDisciplineId) {
        return hoursRepository.findBySpecialtyDisciplineId(specialtyDisciplineId)
                .map(hoursMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Години не знайдено"));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Hours hours = hoursRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Години не знайдено"));
        hoursRepository.delete(hours);
    }

    private SpecialtyDiscipline getSpecialtyDisciplineOrThrow(Long id) {
        return specialtyDisciplineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Дисципліну не знайдено"));
    }
}
