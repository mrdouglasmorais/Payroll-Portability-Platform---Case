package br.com.payroll.portability.portability.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingJobRepository {

  Optional<ProcessingJob> claimNext(String workerId, Instant now, Instant leaseUntil);

  void markSucceeded(UUID jobId, long fenceToken, Instant now);

  void reschedule(
      UUID jobId,
      long fenceToken,
      String jobType,
      Instant nextRunAt,
      int attemptCount,
      String lastError,
      Instant now);

  void markExhausted(UUID jobId, long fenceToken, String lastError, Instant now);

  Optional<String> findIdempotencyKey(UUID operationId);
}
