package org.bachelor.addservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.exception.NotFoundException;
import org.bachelor.addservice.exception.RestException;
import org.bachelor.addservice.mapper.PracticeDetailsMapper;
import org.bachelor.addservice.model.dto.PracticeDetailsCreateDTO;
import org.bachelor.addservice.model.dto.PracticeDetailsDTO;
import org.bachelor.addservice.model.entity.AdditionalWork;
import org.bachelor.addservice.model.entity.PracticeDetails;
import org.bachelor.addservice.repository.AdditionalWorkRepository;
import org.bachelor.addservice.repository.PracticeDetailsRepository;
import org.bachelor.addservice.service.PracticeDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeDetailsServiceImpl implements PracticeDetailsService {

    private final PracticeDetailsRepository practiceDetailsRepository;
    private final AdditionalWorkRepository additionalWorkRepository;
    private final PracticeDetailsMapper practiceDetailsMapper;

    @Override
    public PracticeDetailsDTO getByAdditionalWorkId(Long additionalWorkId) {
        return practiceDetailsMapper.toDTO(findByWorkId(additionalWorkId));
    }

    @Transactional
    @Override
    public PracticeDetailsDTO create(Long additionalWorkId, PracticeDetailsCreateDTO dto) {
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RestException("Дата завершення не може бути раніше дати початку");
        }
        AdditionalWork work = additionalWorkRepository.findById(additionalWorkId)
                .orElseThrow(() -> new NotFoundException("Додаткову роботу не знайдено"));
        if (practiceDetailsRepository.findByAdditionalWorkId(additionalWorkId).isPresent()) {
            throw new RestException("Деталі практики для цього запису вже існують");
        }
        PracticeDetails details = practiceDetailsMapper.toEntity(dto);
        details.setAdditionalWork(work);
        return practiceDetailsMapper.toDTO(practiceDetailsRepository.save(details));
    }

    @Transactional
    @Override
    public PracticeDetailsDTO updateByWorkId(Long additionalWorkId, PracticeDetailsCreateDTO dto) {
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RestException("Дата завершення не може бути раніше дати початку");
        }
        PracticeDetails details = findByWorkId(additionalWorkId);
        details.setOrganization(dto.getOrganization());
        details.setCourse(dto.getCourse());
        details.setStartDate(dto.getStartDate());
        details.setEndDate(dto.getEndDate());
        details.setWorkDescription(dto.getWorkDescription());
        details.setEctsCredits(dto.getEctsCredits());
        details.setTotalHours(dto.getTotalHours());
        details.setSupervisorId(dto.getSupervisorId());
        return practiceDetailsMapper.toDTO(details);
    }

    @Transactional
    @Override
    public void deleteByWorkId(Long additionalWorkId) {
        practiceDetailsRepository.delete(findByWorkId(additionalWorkId));
    }

    private PracticeDetails findByWorkId(Long additionalWorkId) {
        return practiceDetailsRepository.findByAdditionalWorkId(additionalWorkId)
                .orElseThrow(() -> new NotFoundException("Деталі практики не знайдено"));
    }
}
