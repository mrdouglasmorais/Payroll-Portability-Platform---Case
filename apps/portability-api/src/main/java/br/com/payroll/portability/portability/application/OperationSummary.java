package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.portability.domain.BusinessStatus;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import java.time.Instant;
import java.util.UUID;

/** Lightweight list projection — no installment ids. */
public record OperationSummary(
    UUID id,
    BusinessStatus businessStatus,
    Instant createdAt,
    String correlationId,
    String centralProtocol,
    ReceivingAccount receivingAccount) {}
