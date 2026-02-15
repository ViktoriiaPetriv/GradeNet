package org.bachelor.orgservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.orgservice.exception.EntityExistsException;
import org.bachelor.orgservice.exception.NotFoundException;
import org.bachelor.orgservice.exception.RestException;
import org.bachelor.orgservice.mapper.OrganizationMapper;
import org.bachelor.orgservice.model.dto.OrganizationDTO;
import org.bachelor.orgservice.model.dto.OrganizationRequestDTO;
import org.bachelor.orgservice.model.entity.OrgType;
import org.bachelor.orgservice.model.entity.Organization;
import org.bachelor.orgservice.repository.OrganizationRepository;
import org.bachelor.orgservice.service.OrganizationService;
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
            throw new EntityExistsException("Organization with this name already exists");
        }

        validateParentForDepartment(dto);

        Organization organization = organizationMapper.toEntity(dto);
        organization.setParent(resolveParent(dto.parentId(), dto.orgType(), null));

        return organizationMapper.toDto(organizationRepository.save(organization));
    }

    @Override
    public OrganizationDTO update(Long id, OrganizationRequestDTO dto) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (!organization.getName().equals(dto.name())
                && organizationRepository.findByName(dto.name()).isPresent()) {
            throw new EntityExistsException("Organization with this name already exists");
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
                .orElseThrow(() -> new NotFoundException("Parent not found"));

        if (parent.getId().equals(currentOrgId)) {
            throw new RestException("Organization cannot be its own parent");
        }

        if (currentOrgId != null) {
            checkCircularDependency(parent, currentOrgId);
        }

        if (orgType == OrgType.FACULTY && parent.getOrgType() == OrgType.DEPARTMENT) {
            throw new RestException("Department cannot be parent for a faculty");
        }

        if (orgType == OrgType.FACULTY && parent.getOrgType() == OrgType.FACULTY) {
            throw new RestException("Faculty cannot be parent for another faculty");
        }

        return parent;
    }

    @Override
    public OrganizationDTO getById(Long id) {
        return organizationRepository.findById(id)
                .map(organizationMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    @Override
    public void delete(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (organizationRepository.existsByParentId(id)) {
            throw new RestException("Cannot delete organization with child organizations");
        }

        organizationRepository.delete(organization);
    }

    @Override
    public List<OrganizationDTO> getAll() {
        return organizationRepository.findAll().stream().map(organizationMapper::toDto).toList();
    }

    private void validateParentForDepartment(OrganizationRequestDTO dto) {
        if (dto.orgType() == OrgType.DEPARTMENT && dto.parentId() == null) {
            throw new RestException("Department must have a parent organization");
        }
    }

    private void checkCircularDependency(Organization parent, Long currentOrgId) {
        Organization current = parent;
        while (current != null) {
            if (current.getId().equals(currentOrgId)) {
                throw new RestException("Circular parent dependency detected");
            }
            current = current.getParent();
        }
    }
}
