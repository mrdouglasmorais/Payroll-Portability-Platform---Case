package br.com.payroll.portability.portability.adapter.web.dto;

import java.util.List;

public record OperationPageResponse(
    List<OperationListItemResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious) {}
