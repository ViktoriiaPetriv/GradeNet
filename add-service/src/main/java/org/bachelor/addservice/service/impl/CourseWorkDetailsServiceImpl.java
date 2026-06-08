package org.bachelor.addservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.exception.NotFoundException;
import org.bachelor.addservice.exception.RestException;
import org.bachelor.addservice.mapper.CourseWorkDetailsMapper;
import org.bachelor.addservice.model.dto.CourseWorkDetailsCreateDTO;
import org.bachelor.addservice.model.dto.CourseWorkDetailsDTO;
import org.bachelor.addservice.model.entity.AdditionalWork;
import org.bachelor.addservice.model.entity.CourseWorkDetails;
import org.bachelor.addservice.repository.AdditionalWorkRepository;
import org.bachelor.addservice.repository.CourseWorkDetailsRepository;
import org.bachelor.addservice.service.CourseWorkDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseWorkDetailsServiceImpl implements CourseWorkDetailsService {

    private final CourseWorkDetailsRepository courseWorkDetailsRepository;
    private final AdditionalWorkRepository additionalWorkRepository;
    private final CourseWorkDetailsMapper courseWorkDetailsMapper;

    @Override
    public CourseWorkDetailsDTO getByAdditionalWorkId(Long additionalWorkId) {
        return courseWorkDetailsMapper.toDTO(findByWorkId(additionalWorkId));
    }

    @Transactional
    @Override
    public CourseWorkDetailsDTO create(Long additionalWorkId, CourseWorkDetailsCreateDTO dto) {
        AdditionalWork work = additionalWorkRepository.findById(additionalWorkId)
                .orElseThrow(() -> new NotFoundException("Додаткову роботу не знайдено"));
        if (courseWorkDetailsRepository.findByAdditionalWorkId(additionalWorkId).isPresent()) {
            throw new RestException("Деталі курсової роботи для цього запису вже існують");
        }
        CourseWorkDetails details = courseWorkDetailsMapper.toEntity(dto);
        details.setAdditionalWork(work);
        return courseWorkDetailsMapper.toDTO(courseWorkDetailsRepository.save(details));
    }

    @Transactional
    @Override
    public CourseWorkDetailsDTO updateByWorkId(Long additionalWorkId, CourseWorkDetailsCreateDTO dto) {
        CourseWorkDetails details = findByWorkId(additionalWorkId);
        details.setSemester(dto.getSemester());
        details.setState(dto.getState());
        details.setEctsCredits(dto.getEctsCredits());
        details.setTotalHours(dto.getTotalHours());
        return courseWorkDetailsMapper.toDTO(details);
    }

    @Transactional
    @Override
    public void deleteByWorkId(Long additionalWorkId) {
        courseWorkDetailsRepository.delete(findByWorkId(additionalWorkId));
    }

    private CourseWorkDetails findByWorkId(Long additionalWorkId) {
        return courseWorkDetailsRepository.findByAdditionalWorkId(additionalWorkId)
                .orElseThrow(() -> new NotFoundException("Деталі курсової роботи не знайдено"));
    }
}
