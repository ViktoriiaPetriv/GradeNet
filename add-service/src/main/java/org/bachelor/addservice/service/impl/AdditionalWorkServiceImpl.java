package org.bachelor.addservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.exception.NotFoundException;
import org.bachelor.addservice.mapper.AdditionalWorkMapper;
import org.bachelor.addservice.model.dto.AdditionalWorkCreateDTO;
import org.bachelor.addservice.model.dto.AdditionalWorkDTO;
import org.bachelor.addservice.model.dto.AuthenticatedUser;
import org.bachelor.addservice.model.dto.GradeWorkDTO;
import org.bachelor.addservice.model.entity.AdditionalWork;
import org.bachelor.addservice.model.entity.Commission;
import org.bachelor.addservice.repository.AdditionalWorkRepository;
import org.bachelor.addservice.repository.CommissionRepository;
import org.bachelor.addservice.service.AdditionalWorkService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdditionalWorkServiceImpl implements AdditionalWorkService {

    private final AdditionalWorkRepository additionalWorkRepository;
    private final CommissionRepository commissionRepository;
    private final AdditionalWorkMapper additionalWorkMapper;

    @Override
    public List<AdditionalWorkDTO> getAll() {
        return additionalWorkRepository.findAll()
                .stream()
                .map(additionalWorkMapper::toDTO)
                .toList();
    }

    @Override
    public AdditionalWorkDTO getById(Long id) {
        return additionalWorkMapper.toDTO(findById(id));
    }

    @Override
    public List<AdditionalWorkDTO> getByCommissionId(Long commissionId) {
        return additionalWorkRepository.findAllByCommissionId(commissionId)
                .stream()
                .map(additionalWorkMapper::toDTO)
                .toList();
    }

    @Override
    public List<AdditionalWorkDTO> getByBookNumberId(Long bookNumberId) {
        return additionalWorkRepository.findAllByBookNumberId(bookNumberId)
                .stream()
                .map(additionalWorkMapper::toDTO)
                .toList();
    }

    @Transactional
    @Override
    public AdditionalWorkDTO create(AdditionalWorkCreateDTO dto) {
        Commission commission = commissionRepository.findById(dto.getCommissionId())
                .orElseThrow(() -> new NotFoundException("Комісію не знайдено"));
        AdditionalWork work = additionalWorkMapper.toEntity(dto);
        work.setCommission(commission);
        return additionalWorkMapper.toDTO(additionalWorkRepository.save(work));
    }

    @Transactional
    @Override
    public AdditionalWorkDTO update(Long id, AdditionalWorkCreateDTO dto) {
        AdditionalWork work = findById(id);
        if (!work.getCommission().getId().equals(dto.getCommissionId())) {
            Commission commission = commissionRepository.findById(dto.getCommissionId())
                    .orElseThrow(() -> new NotFoundException("Комісію не знайдено"));
            work.setCommission(commission);
        }
        work.setBookNumberId(dto.getBookNumberId());
        work.setType(dto.getType());
        work.setTitle(dto.getTitle());
        work.setEventDate(dto.getEventDate());
        work.setUniversityGrade(dto.getUniversityGrade());
        work.setNationalGrade(dto.getNationalGrade());
        work.setEctsGrade(dto.getEctsGrade());
        return additionalWorkMapper.toDTO(work);
    }

    @Transactional
    @Override
    public AdditionalWorkDTO grade(Long id, GradeWorkDTO dto, AuthenticatedUser user) {
        AdditionalWork work = findById(id);
        checkGradePermission(work.getCommission(), user);
        work.setUniversityGrade(dto.getUniversityGrade());
        work.setEctsGrade(dto.getEctsGrade());
        work.setNationalGrade(dto.getNationalGrade());
        return additionalWorkMapper.toDTO(work);
    }

    private void checkGradePermission(Commission commission, AuthenticatedUser user) {
        if (user.isAdmin()) return;
        if (user.isManager()) {
            if (commission.getOrgId() != null && commission.getOrgId().equals(user.orgId())) return;
            throw new AccessDeniedException("Доступ заборонено");
        }
        if (user.isProfessor()) {
            boolean isMember = commission.getMembers().stream()
                    .anyMatch(m -> m.getProfessorId().equals(user.userId()));
            if (isMember) return;
        }
        throw new AccessDeniedException("Доступ заборонено");
    }

    @Transactional
    @Override
    public void delete(Long id) {
        AdditionalWork work = findById(id);
        additionalWorkRepository.delete(work);
    }

    private AdditionalWork findById(Long id) {
        return additionalWorkRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Додаткову роботу не знайдено"));
    }
}
