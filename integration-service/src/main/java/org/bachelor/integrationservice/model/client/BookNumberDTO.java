package org.bachelor.integrationservice.model.client;

import lombok.Data;

@Data
public class BookNumberDTO {
    private Long id;
    private String number;
    private Long studentId;
    private Long specialtyId;
    private String status;
}
