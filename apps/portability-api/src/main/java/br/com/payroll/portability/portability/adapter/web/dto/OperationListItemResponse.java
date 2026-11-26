package br.com.payroll.portability.portability.adapter.web.dto;

public record OperationListItemResponse(
    String operationId,
    String status,
    String createdAt,
    String correlationId,
    String centralProtocol,
    ReceivingAccountView contaRecebimento,
    boolean contaEfetivada) {}
