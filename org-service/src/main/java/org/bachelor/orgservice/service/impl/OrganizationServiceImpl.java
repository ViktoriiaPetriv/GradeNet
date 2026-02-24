package org.bachelor.orgservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.orgservice.exception.EntityExistsException;
import org.bachelor.orgservice.exception.NotFoundException;
import org.bachelor.orgservice.exception.RestException;
import org.bachelor.orgservice.mapper.OrganizationMapper;
import org.bachelor.orgservice.model.dto.OrganizationDTO;
import org.bachelor.orgservice.model.dto.OrganizationRequestDTO;
import org.bachelor.orgservice.model.dto.OrganizationShortDTO;
import org.bachelor.orgservice.model.dto.PageResponse;
import org.bachelor.orgservice.model.entity.OrgType;
import org.bachelor.orgservice.model.entity.Organization;
import org.bachelor.orgservice.repository.OrganizationRepository;
import org.bachelor.orgservice.service.OrganizationService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    @Override
    public OrganizationDTO create(OrganizationRequestDTO dto) {
        if (organizationRepository.findByName(dto.name()).isPresent()) {
            throw new EntityExistsException("Організація з такою назвою вже існує");
        }

        validateParentForDepartment(dto);

        Organization organization = organizationMapper.toEntity(dto);
        organization.setParent(resolveParent(dto.parentId(), dto.orgType(), null));

        return organizationMapper.toDto(organizationRepository.save(organization));
    }

    @Override
    public OrganizationDTO update(Long id, OrganizationRequestDTO dto) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Організацію не знайдено"));

        if (!organization.getName().equals(dto.name())
                && organizationRepository.findByName(dto.name()).isPresent()) {
            throw new EntityExistsException("Організація з такою назвою вже існує");
        }

        validateParentForDepartment(dto);

        organization.setName(dto.name());
        organization.setOrgType(dto.orgType());
        organization.setParent(resolveParent(dto.parentId(), dto.orgType(), id));

        return organizationMapper.toDto(organizationRepository.save(organization));
    }

    private Organization resolveParent(Long parentId, OrgType orgType, Long currentOrgId) {
        if (parentId == null) return null;

        Organization parent = organizationRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Батьківську організацію не знайдено"));

        if (parent.getId().equals(currentOrgId)) {
            throw new RestException("Організація не може бути батьківською сама для себе");
        }

        if (currentOrgId != null) {
            checkCircularDependency(parent, currentOrgId);
        }

        if (orgType == OrgType.FACULTY && parent.getOrgType() == OrgType.DEPARTMENT) {
            throw new RestException("Кафедра не може бути батьківською для факультету");
        }

        if (orgType == OrgType.FACULTY && parent.getOrgType() == OrgType.FACULTY) {
            throw new RestException("Факультет не може бути батьківською організацією для іншого факультету");
        }

        return parent;
    }

    @Override
    public OrganizationDTO getById(Long id) {
        return organizationRepository.findById(id)
                .map(organizationMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Організацію не знайдено"));
    }

    @Override
    public void delete(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Організацію не знайдено"));

        if (organizationRepository.existsByParentId(id)) {
            throw new RestException("Неможливо видалити організацію, яка містить підпорядковані організації");
        }

        organizationRepository.delete(organization);
    }

    @Override
    public PageResponse<OrganizationDTO> getAll(OrgType orgType, Pageable pageable) {
        if (orgType != null) {
            return PageResponse.of(
                    organizationRepository.findAllByOrgType(orgType, pageable)
                            .map(organizationMapper::toDto)
            );
        }
        return PageResponse.of(
                organizationRepository.findAll(pageable)
                        .map(organizationMapper::toDto)
        );
    }

    @Override
    public List<OrganizationShortDTO> getAllShort(OrgType orgType) {
        if (orgType != null) {
            return organizationRepository.findAllByOrgType(orgType)
                    .stream()
                    .map(organizationMapper::toShortDto)
                    .toList();
        }
        return organizationRepository.findAll()
                .stream()
                .map(organizationMapper::toShortDto)
                .toList();
    }

    private void validateParentForDepartment(OrganizationRequestDTO dto) {
        if (dto.orgType() == OrgType.DEPARTMENT && dto.parentId() == null) {
            throw new RestException("Кафедра повинна мати батьківську організацію");
        }
    }

    private void checkCircularDependency(Organization parent, Long currentOrgId) {
        Organization current = parent;
        while (current != null) {
            if (current.getId().equals(currentOrgId)) {
                throw new RestException("Виявлено циклічну залежність між організаціями");
            }
            current = current.getParent();
        }
    }
}
