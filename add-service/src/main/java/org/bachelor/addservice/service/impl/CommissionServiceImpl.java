package org.bachelor.addservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.addservice.exception.NotFoundException;
import org.bachelor.addservice.exception.RestException;
import org.bachelor.addservice.mapper.CommissionMapper;
import org.bachelor.addservice.mapper.CommissionMemberMapper;
import org.bachelor.addservice.model.dto.AuthenticatedUser;
import org.bachelor.addservice.model.dto.CommissionCreateDTO;
import org.bachelor.addservice.model.dto.CommissionDTO;
import org.bachelor.addservice.model.dto.CommissionMemberCreateDTO;
import org.bachelor.addservice.model.dto.CommissionMemberDTO;
import org.bachelor.addservice.model.dto.PageResponse;
import org.bachelor.addservice.model.entity.Commission;
import org.bachelor.addservice.model.entity.CommissionMember;
import org.bachelor.addservice.repository.CommissionMemberRepository;
import org.bachelor.addservice.repository.CommissionRepository;
import org.bachelor.addservice.service.CommissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommissionServiceImpl implements CommissionService {

    private final CommissionRepository commissionRepository;
    private final CommissionMemberRepository memberRepository;
    private final CommissionMapper commissionMapper;
    private final CommissionMemberMapper memberMapper;

    @Override
    public List<CommissionDTO> getAll(AuthenticatedUser user) {
        if (user.isProfessor()) {
            return commissionRepository.findAllByProfessor(user.userId())
                    .stream()
                    .map(commissionMapper::toDTO)
                    .toList();
        }
        return commissionRepository.findAll()
                .stream()
                .map(commissionMapper::toDTO)
                .toList();
    }

    @Override
    public PageResponse<CommissionDTO> getPage(AuthenticatedUser user, int page, int size, String status, String sortBy, String sortDir) {
        Sort sort = buildSort(sortBy, sortDir, "startDate");
        PageRequest pageable = PageRequest.of(page, size, sort);
        Page<Commission> result;
        if (user.isProfessor()) {
            result = commissionRepository.findPagedByProfessor(user.userId(), status, pageable);
        } else {
            result = commissionRepository.findPagedAll(status, pageable);
        }
        return PageResponse.of(result.map(commissionMapper::toDTO));
    }

    @Override
    public CommissionDTO getById(Long id) {
        return commissionMapper.toDTO(findCommissionById(id));
    }

    @Transactional
    @Override
    public CommissionDTO create(CommissionCreateDTO dto, Long orgId) {
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RestException("Дата завершення не може бути раніше дати початку");
        }
        Commission commission = commissionMapper.toEntity(dto);
        commission.setOrgId(orgId);
        return commissionMapper.toDTO(commissionRepository.save(commission));
    }

    @Transactional
    @Override
    public CommissionDTO update(Long id, CommissionCreateDTO dto) {
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RestException("Дата завершення не може бути раніше дати початку");
        }
        Commission commission = findCommissionById(id);
        commission.setStartDate(dto.getStartDate());
        commission.setEndDate(dto.getEndDate());
        return commissionMapper.toDTO(commission);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Commission commission = findCommissionById(id);
        commissionRepository.delete(commission);
    }

    @Transactional
    @Override
    public CommissionMemberDTO addMember(Long commissionId, CommissionMemberCreateDTO dto) {
        Commission commission = findCommissionById(commissionId);
        if (memberRepository.existsByCommissionIdAndProfessorId(commissionId, dto.getProfessorId())) {
            throw new RestException("Викладач вже є членом цієї комісії");
        }
        if (Boolean.TRUE.equals(dto.getIsHead()) && memberRepository.existsByCommissionIdAndIsHeadTrue(commissionId)) {
            throw new RestException("Комісія вже має голову");
        }
        CommissionMember member = memberMapper.toEntity(dto);
        member.setCommission(commission);
        return memberMapper.toDTO(memberRepository.save(member));
    }

    @Transactional
    @Override
    public void removeMember(Long commissionId, Long memberId) {
        CommissionMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Члена комісії не знайдено"));
        if (!member.getCommission().getId().equals(commissionId)) {
            throw new RestException("Член комісії не належить до вказаної комісії");
        }
        memberRepository.delete(member);
    }

    private Commission findCommissionById(Long id) {
        return commissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Комісію не знайдено"));
    }

    private Sort buildSort(String sortBy, String sortDir, String defaultField) {
        String field = (sortBy == null || sortBy.isBlank()) ? defaultField : sortBy;
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
}
