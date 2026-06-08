package org.bachelor.addservice.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CommissionDTO {
    private Long id;
    private Long orgId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<CommissionMemberDTO> members;
}
