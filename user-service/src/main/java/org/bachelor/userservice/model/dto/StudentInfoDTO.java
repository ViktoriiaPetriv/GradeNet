package org.bachelor.userservice.model.dto;

import java.time.Instant;

public record StudentInfoDTO(
        Long bookId,
        String bookNumber,
        String bookNumberStatus,
        Instant startDate,
        Instant endDate,
        Long specialtyOfferingId,
        Long orgId
) {
}
