package org.bachelor.addservice.model.dto;

import lombok.Data;

@Data
public class CommissionMemberDTO {
    private Long id;
    private Long commissionId;
    private Long professorId;
    private Boolean isHead;
}
