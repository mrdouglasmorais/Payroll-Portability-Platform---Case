package br.com.payroll.portability.portability.adapter.web.dto;

public record OperationEventResponse(
    String eventType, long sequence, String occurredAt, String description) {}
