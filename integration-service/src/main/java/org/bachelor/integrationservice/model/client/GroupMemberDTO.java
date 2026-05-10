package org.bachelor.integrationservice.model.client;

import lombok.Data;

@Data
public class GroupMemberDTO {
    private Long bookNumberId;
    private Long studentGroupId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
}
