package org.bachelor.userservice.model.dto;

import java.time.Instant;

public record StudentInfoDTO(
        String bookNumber,
        String bookNumberStatus,
        Instant startDate,
        Instant endDate,
        Long specialtyId,
        Long orgId
) {
}
