package org.bachelor.userservice.model.dto;

import lombok.Data;

@Data
public class StudentGroupMemberDTO {
    private Long bookNumberId;
    private Long studentGroupId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
}
