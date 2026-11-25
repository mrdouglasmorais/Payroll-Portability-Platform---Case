package br.com.payroll.portability.portability.domain;

import java.util.Set;
import java.util.UUID;

public record PortabilityCommand(
    UUID beneficiaryId,
    Set<String> externalInstallmentIds,
    String assigneeCreditor,
    ReceivingAccount receivingAccount,
    String idempotencyKey,
    String correlationId,
    String requestHash) {}
