package org.bachelor.orgservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.orgservice.config.GradeServiceClient;
import org.bachelor.orgservice.exception.EntityExistsException;
import org.bachelor.orgservice.exception.NotFoundException;
import org.bachelor.orgservice.exception.RestException;
import org.bachelor.orgservice.mapper.SpecialtyOfferingMapper;
import org.bachelor.orgservice.model.dto.SpecialtyOfferingDTO;
import org.bachelor.orgservice.model.dto.SpecialtyOfferingRequestDTO;
import org.bachelor.orgservice.model.entity.Specialty;
import org.bachelor.orgservice.model.entity.SpecialtyOffering;
import org.bachelor.orgservice.repository.SpecialtyOfferingRepository;
import org.bachelor.orgservice.repository.SpecialtyRepository;
import org.bachelor.orgservice.service.SpecialtyOfferingService;
import org.bachelor.orgservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SpecialtyOfferingServiceImpl implements SpecialtyOfferingService {

    private final SpecialtyOfferingRepository specialtyOfferingRepository;
    private final SpecialtyRepository specialtyRepository;
    private final SpecialtyOfferingMapper mapper;
    private final GradeServiceClient gradeServiceClient;

    @Override
    public SpecialtyOfferingDTO create(SpecialtyOfferingRequestDTO request) {
        SecurityUtils.requireAdminOrManager();

        Specialty specialty = specialtyRepository.findById(request.specialtyId())
                .orElseThrow(() -> new NotFoundException("Спеціальність не знайдено"));

        if (specialtyOfferingRepository.existsBySpecialtyIdAndGraduationYear(
                request.specialtyId(), request.graduationYear())) {
            throw new EntityExistsException("Набір для цієї спеціальності та року вже існує");
        }

        SpecialtyOffering offering = new SpecialtyOffering();
        offering.setSpecialty(specialty);
        offering.setExternalId(request.externalId());
        offering.setGraduationYear(request.graduationYear());

        return mapper.toDto(specialtyOfferingRepository.save(offering));
    }

    @Override
    public SpecialtyOfferingDTO getById(Long id) {
        return specialtyOfferingRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException("Набір не знайдено"));
    }

    @Override
    public List<SpecialtyOfferingDTO> getAllBySpecialty(Long specialtyId) {
        return specialtyOfferingRepository.findAllBySpecialtyId(specialtyId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<Long> getIdsBySpecialtyIds(List<Long> specialtyIds) {
        if (specialtyIds == null || specialtyIds.isEmpty()) return List.of();
        return specialtyOfferingRepository.findIdsBySpecialtyIdIn(specialtyIds);
    }

    @Override
    public void delete(Long id) {
        SecurityUtils.requireAdmin();
        SpecialtyOffering offering = specialtyOfferingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Набір не знайдено"));

        if (gradeServiceClient.hasSpecialtyDisciplines(id)) {
            throw new RestException("Неможливо видалити рік випуску: існують прив'язані дисципліни або залікові книжки");
        }

        specialtyOfferingRepository.delete(offering);
    }
}
