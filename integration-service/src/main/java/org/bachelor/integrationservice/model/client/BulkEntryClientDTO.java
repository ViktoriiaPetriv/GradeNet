package org.bachelor.integrationservice.model.client;

import lombok.Data;

@Data
public class BulkEntryClientDTO {
    private Long entryId;
    private Long bookNumberId;
    private String status;
    private String result;
    private GradeValueDTO latestGrade;

    @Data
    public static class GradeValueDTO {
        private Integer universityGrade;
    }
}
