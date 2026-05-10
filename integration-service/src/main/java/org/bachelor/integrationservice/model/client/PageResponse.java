package org.bachelor.integrationservice.model.client;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
}
