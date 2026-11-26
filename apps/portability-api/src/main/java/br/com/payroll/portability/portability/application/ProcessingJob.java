package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.portability.domain.TechnicalJobStatus;
import java.time.Instant;
import java.util.UUID;

public record ProcessingJob(
    UUID id,
    UUID operationId,
    String jobType,
    TechnicalJobStatus technicalStatus,
    int attemptCount,
    String leaseOwner,
    Instant leaseUntil,
    long fenceToken,
    Instant nextRunAt,
    String lastError,
    String lastErrorCode) {}
