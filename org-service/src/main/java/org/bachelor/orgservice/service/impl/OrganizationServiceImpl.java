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
        if (organizationRepository.findByName(dto.getName()).isPresent()) {
            throw new EntityExistsException("Organization with this name already exists");
        }

        validateParentForDepartment(dto);

        Organization organization = organizationMapper.toEntity(dto);
        organization.setParent(resolveParent(dto.getParentId(), dto.getOrgType(), null));

        return organizationMapper.toDto(organizationRepository.save(organization));
    }

    @Override
    public OrganizationDTO update(Long id, OrganizationRequestDTO dto) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (!organization.getName().equals(dto.getName())
                && organizationRepository.findByName(dto.getName()).isPresent()) {
            throw new EntityExistsException("Organization with this name already exists");
        }

        validateParentForDepartment(dto);

        organization.setName(dto.getName());
        organization.setOrgType(dto.getOrgType());
        organization.setParent(resolveParent(dto.getParentId(), dto.getOrgType(), id));

        return organizationMapper.toDto(organizationRepository.save(organization));
    }

    private Organization resolveParent(Long parentId, OrgType orgType, Long currentOrgId) {
        if (parentId == null) return null;

        Organization parent = organizationRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Parent not found"));

        if (parent.getId().equals(currentOrgId)) {
            throw new RestException("Organization cannot be its own parent");
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
        organizationRepository.delete(organization);
    }

    @Override
    public List<OrganizationDTO> getAll() {
        return organizationRepository.findAll().stream().map(organizationMapper::toDto).toList();
    }

    private void validateParentForDepartment(OrganizationRequestDTO dto) {
        if (dto.getOrgType() == OrgType.DEPARTMENT && dto.getParentId() == null) {
            throw new RestException("Department must have a parent organization");
        }
    }
}
