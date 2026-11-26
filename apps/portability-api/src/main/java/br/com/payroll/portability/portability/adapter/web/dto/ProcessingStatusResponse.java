package br.com.payroll.portability.portability.adapter.web.dto;

public record ProcessingStatusResponse(
    String status, int attemptCount, String errorCode, boolean requiresAction) {}
