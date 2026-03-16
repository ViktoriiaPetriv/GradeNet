package org.bachelor.orgservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.orgservice.exception.EntityExistsException;
import org.bachelor.orgservice.exception.NotFoundException;
import org.bachelor.orgservice.exception.RestException;
import org.bachelor.orgservice.mapper.SpecialtyMapper;
import org.bachelor.orgservice.model.dto.AuthenticatedUser;
import org.bachelor.orgservice.model.dto.OrgInfoDTO;
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
import org.bachelor.orgservice.utils.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyRepository specialtyRepository;
    private final OrganizationRepository organizationRepository;
    private final SpecialtyMapper specialtyMapper;

    @Override
    public SpecialtyDTO create(SpecialtyRequestDTO dto) {
        SecurityUtils.requireAdmin();
        validateUniqueness(dto, null);
        validateDates(dto);

        Organization organization = resolveOrganization(dto.orgId());

        Specialty specialty = specialtyMapper.toEntity(dto);
        specialty.setOrganization(organization);

        return specialtyMapper.toDto(specialtyRepository.save(specialty));
    }

    @Override
    public SpecialtyDTO update(Long id, SpecialtyRequestDTO dto) {
        SecurityUtils.requireAdmin();
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Спеціальність не знайдено"));

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
        SecurityUtils.requireAdminOrManager();

        return specialtyRepository.findById(id)
                .map(specialtyMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Спеціальність не знайдено"));
    }

    @Override
    public PageResponse<SpecialtyDTO> getAll(Degree degree, EduType eduType, Pageable pageable) {
        SecurityUtils.requireAdminOrManager();

        Specification<Specialty> spec = SpecialtySpecification.hasDegree(degree)
                .and(SpecialtySpecification.hasEduType(eduType));

        return PageResponse.of(specialtyRepository.findAll(spec, pageable).map(specialtyMapper::toDto));
    }

    @Override
    public PageResponse<SpecialtyDTO> getAllByOrganization(Long orgId, Degree degree, EduType eduType, Pageable pageable) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.isManager()) {
            Long facultyId = currentUser.getOrgId();
            boolean hasAccess = facultyId != null && facultyId.equals(orgId);

            if (!hasAccess) {
                Organization org = organizationRepository.findById(orgId)
                        .orElseThrow(() -> new NotFoundException("Організацію не знайдено"));
                hasAccess = org.getParent() != null
                        && facultyId != null
                        && facultyId.equals(org.getParent().getId());
            }

            if (!hasAccess) {
                throw new AccessDeniedException("Немає доступу до цієї організації");
            }
        } else if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Недостатньо прав");
        }

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Організацію не знайдено"));

        List<Long> orgIds = resolveOrgIds(organization);

        Specification<Specialty> spec = SpecialtySpecification.hasOrganizationIdIn(orgIds)
                .and(SpecialtySpecification.hasDegree(degree))
                .and(SpecialtySpecification.hasEduType(eduType));

        return PageResponse.of(specialtyRepository.findAll(spec, pageable).map(specialtyMapper::toDto));
    }

    @Override
        public void delete(Long id) {
        SecurityUtils.requireAdmin();

        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Спеціальність не знайдено"));

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
                .orElseThrow(() -> new NotFoundException("Організацію не знайдено"));

        if (organization.getOrgType() != OrgType.DEPARTMENT) {
            throw new RestException("Спеціальності можуть бути прив’язані лише до кафедр");
        }

        return organization;
    }

    private void validateDates(SpecialtyRequestDTO dto) {
        if (dto.endDate() != null && !dto.endDate().isAfter(dto.startDate())) {
            throw new RestException("Дата завершення повинна бути пізнішою за дату початку");
        }
    }

    private void validateUniqueness(SpecialtyRequestDTO dto, Long currentId) {
        Specification<Specialty> spec = SpecialtySpecification.hasCodeDegreeEduTypeOrg(
                dto.code(), dto.degree(), dto.eduType(), dto.orgId());

        specialtyRepository.findOne(spec)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(e -> {
                    throw new EntityExistsException(
                            "Спеціальність із таким кодом, рівнем та типом навчання вже існує для цієї кафедри"
                    );
                });
    }

    @Override
    public OrgInfoDTO getOrgInfo(Long specialtyId) {
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new NotFoundException("Specialty not found"));

        Organization department = specialty.getOrganization();

        Organization faculty = organizationRepository.findById(department.getParent().getId())
                .orElseThrow(() -> new NotFoundException("Faculty not found"));

        return new OrgInfoDTO(
                faculty.getId(),
                faculty.getName(),
                department.getId(),
                department.getName()
        );
    }

    @Override
    public List<Long> getIdsByOrgIds(List<Long> orgIds) {
        List<Long> expandedIds = new ArrayList<>(orgIds);

        List<Long> departmentIds = organizationRepository
                .findAllByParentIdInAndOrgType(orgIds, OrgType.DEPARTMENT)
                .stream()
                .map(Organization::getId)
                .toList();

        expandedIds.addAll(departmentIds);

        return specialtyRepository.findAllByOrganizationIdIn(expandedIds)
                .stream()
                .map(Specialty::getId)
                .toList();
    }
}