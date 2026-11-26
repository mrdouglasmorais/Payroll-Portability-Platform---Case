package br.com.payroll.portability.portability.adapter.web.dto;

public record OperationStatusResponse(
    String operationId,
    String status,
    String motivoRejeicao,
    String correlationId,
    String centralProtocol,
    ProcessingStatusResponse processing) {}
