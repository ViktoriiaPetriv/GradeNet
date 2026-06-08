package org.bachelor.addservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.exception.NotFoundException;
import org.bachelor.addservice.exception.RestException;
import org.bachelor.addservice.mapper.QualificationDetailsMapper;
import org.bachelor.addservice.model.dto.QualificationDetailsCreateDTO;
import org.bachelor.addservice.model.dto.QualificationDetailsDTO;
import org.bachelor.addservice.model.entity.AdditionalWork;
import org.bachelor.addservice.model.entity.QualificationDetails;
import org.bachelor.addservice.repository.AdditionalWorkRepository;
import org.bachelor.addservice.repository.QualificationDetailsRepository;
import org.bachelor.addservice.service.QualificationDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualificationDetailsServiceImpl implements QualificationDetailsService {

    private final QualificationDetailsRepository qualificationDetailsRepository;
    private final AdditionalWorkRepository additionalWorkRepository;
    private final QualificationDetailsMapper qualificationDetailsMapper;

    @Override
    public QualificationDetailsDTO getByAdditionalWorkId(Long additionalWorkId) {
        return qualificationDetailsMapper.toDTO(findByWorkId(additionalWorkId));
    }

    @Transactional
    @Override
    public QualificationDetailsDTO create(Long additionalWorkId, QualificationDetailsCreateDTO dto) {
        AdditionalWork work = additionalWorkRepository.findById(additionalWorkId)
                .orElseThrow(() -> new NotFoundException("Додаткову роботу не знайдено"));
        if (qualificationDetailsRepository.findByAdditionalWorkId(additionalWorkId).isPresent()) {
            throw new RestException("Деталі кваліфікаційної роботи для цього запису вже існують");
        }
        QualificationDetails details = qualificationDetailsMapper.toEntity(dto);
        details.setAdditionalWork(work);
        return qualificationDetailsMapper.toDTO(qualificationDetailsRepository.save(details));
    }

    @Transactional
    @Override
    public QualificationDetailsDTO updateByWorkId(Long additionalWorkId, QualificationDetailsCreateDTO dto) {
        QualificationDetails details = findByWorkId(additionalWorkId);
        details.setSupervisorId(dto.getSupervisorId());
        details.setState(dto.getState());
        return qualificationDetailsMapper.toDTO(details);
    }

    @Transactional
    @Override
    public void deleteByWorkId(Long additionalWorkId) {
        qualificationDetailsRepository.delete(findByWorkId(additionalWorkId));
    }

    private QualificationDetails findByWorkId(Long additionalWorkId) {
        return qualificationDetailsRepository.findByAdditionalWorkId(additionalWorkId)
                .orElseThrow(() -> new NotFoundException("Деталі кваліфікаційної роботи не знайдено"));
    }
}
