package br.com.payroll.portability.portability.domain;

import java.time.Instant;
import java.util.UUID;

public record OperationEvent(
    UUID id,
    UUID operationId,
    int sequenceNo,
    String eventType,
    String description,
    String correlationId,
    Instant occurredAt) {}
