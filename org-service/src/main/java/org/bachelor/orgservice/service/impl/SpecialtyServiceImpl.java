package org.bachelor.orgservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.orgservice.exception.EntityExistsException;
import org.bachelor.orgservice.exception.NotFoundException;
import org.bachelor.orgservice.exception.RestException;
import org.bachelor.orgservice.mapper.SpecialtyMapper;
import org.bachelor.orgservice.model.dto.PageResponse;
import org.bachelor.orgservice.model.dto.SpecialtyDTO;
import org.bachelor.orgservice.model.dto.SpecialtyRequestDTO;
import org.bachelor.orgservice.model.entity.Degree;
import org.bachelor.orgservice.model.entity.EduType;
import org.bachelor.orgservice.model.entity.OrgType;
import org.bachelor.orgservice.model.entity.Organization;
import org.bachelor.orgservice.model.entity.Specialty;
import org.bachelor.orgservice.repository.OrganizationRepository;
import org.bachelor.orgservice.repository.SpecialtyRepository;
import org.bachelor.orgservice.repository.SpecialtySpecification;
import org.bachelor.orgservice.service.SpecialtyService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyRepository specialtyRepository;
    private final OrganizationRepository organizationRepository;
    private final SpecialtyMapper specialtyMapper;

    @Override
    public SpecialtyDTO create(SpecialtyRequestDTO dto) {
        validateUniqueness(dto, null);
        validateDates(dto);

        Organization organization = resolveOrganization(dto.orgId());

        Specialty specialty = specialtyMapper.toEntity(dto);
        specialty.setOrganization(organization);

        return specialtyMapper.toDto(specialtyRepository.save(specialty));
    }

    @Override
    public SpecialtyDTO update(Long id, SpecialtyRequestDTO dto) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Specialty not found"));

        validateUniqueness(dto, id);
        validateDates(dto);

        Organization organization = resolveOrganization(dto.orgId());

        specialty.setCode(dto.code());
        specialty.setNameUA(dto.nameUA());
        specialty.setNameEN(dto.nameEN());
        specialty.setStudyProgramUA(dto.studyProgramUA());
        specialty.setStudyProgramEN(dto.studyProgramEN());
        specialty.setEduProgramUA(dto.eduProgramUA());
        specialty.setEduProgramEN(dto.eduProgramEN());
        specialty.setOrganization(organization);
        specialty.setDegree(dto.degree());
        specialty.setEduType(dto.eduType());
        specialty.setStartDate(dto.startDate());
        specialty.setEndDate(dto.endDate());

        return specialtyMapper.toDto(specialtyRepository.save(specialty));
    }

    @Override
    public SpecialtyDTO getById(Long id) {
        return specialtyRepository.findById(id)
                .map(specialtyMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Specialty not found"));
    }

    @Override
    public PageResponse<SpecialtyDTO> getAll(Degree degree, EduType eduType, Pageable pageable) {
        Specification<Specialty> spec = SpecialtySpecification.hasDegree(degree)
                .and(SpecialtySpecification.hasEduType(eduType));

        return PageResponse.of(specialtyRepository.findAll(spec, pageable).map(specialtyMapper::toDto));
    }

    @Override
    public PageResponse<SpecialtyDTO> getAllByOrganization(Long orgId, Degree degree, EduType eduType, Pageable pageable) {
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        List<Long> orgIds = resolveOrgIds(organization);

        Specification<Specialty> spec = SpecialtySpecification.hasOrganizationIdIn(orgIds)
                .and(SpecialtySpecification.hasDegree(degree))
                .and(SpecialtySpecification.hasEduType(eduType));

        return PageResponse.of(specialtyRepository.findAll(spec, pageable).map(specialtyMapper::toDto));
    }

    @Override
        public void delete(Long id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Specialty not found"));

        specialtyRepository.delete(specialty);
    }

    private List<Long> resolveOrgIds(Organization organization) {
        return switch (organization.getOrgType()) {
            case DEPARTMENT -> List.of(organization.getId());
            case FACULTY -> organizationRepository
                    .findAllByParentIdAndOrgType(organization.getId(), OrgType.DEPARTMENT)
                    .stream()
                    .map(Organization::getId)
                    .toList();
        };
    }

    private Organization resolveOrganization(Long orgId) {
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (organization.getOrgType() != OrgType.DEPARTMENT) {
            throw new RestException("Specialties can only be assigned to departments");
        }

        return organization;
    }

    private void validateDates(SpecialtyRequestDTO dto) {
        if (dto.endDate() != null && !dto.endDate().isAfter(dto.startDate())) {
            throw new RestException("End date must be after start date");
        }
    }

    private void validateUniqueness(SpecialtyRequestDTO dto, Long currentId) {
        specialtyRepository.findByCode(dto.code())
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(e -> { throw new EntityExistsException("Specialty with this code already exists"); });

        specialtyRepository.findByNameUA(dto.nameUA())
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(e -> { throw new EntityExistsException("Specialty with this name (UA) already exists"); });

        specialtyRepository.findByNameEN(dto.nameEN())
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(e -> { throw new EntityExistsException("Specialty with this name (EN) already exists"); });
    }
}