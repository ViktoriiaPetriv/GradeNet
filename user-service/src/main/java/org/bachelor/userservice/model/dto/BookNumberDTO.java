package org.bachelor.userservice.model.dto;

import lombok.Data;
import org.bachelor.userservice.model.entity.BookNumberStatus;

import java.time.Instant;

@Data
public class BookNumberDTO {
    private Long id;
    private String number;
    private Long studentId;
    private Instant regStartDate;
    private Instant regEndDate;
    private Instant handedDate;
    private BookNumberStatus status;
    private Long specialtyId;
}
