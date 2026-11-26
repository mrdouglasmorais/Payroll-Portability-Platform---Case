package br.com.payroll.portability.portability.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingJobRepository {

  Optional<ProcessingJob> claimNext(String workerId, Instant now, Instant leaseUntil);

  /** Locks the job row for the duration of the current transaction; empty if fence lost. */
  boolean lockOwnedFence(UUID jobId, long fenceToken, String workerId);

  boolean stillOwns(UUID jobId, long fenceToken, String workerId);

  void markSucceeded(UUID jobId, long fenceToken, Instant now);

  int markSucceededIfOwned(UUID jobId, long fenceToken, String workerId, Instant now);

  void reschedule(
      UUID jobId,
      long fenceToken,
      String jobType,
      Instant nextRunAt,
      int attemptCount,
      String lastError,
      String lastErrorCode,
      Instant now);

  void markExhausted(
      UUID jobId, long fenceToken, String lastError, String lastErrorCode, Instant now);

  Optional<ProcessingJob> findByOperationId(UUID operationId);

  Optional<String> findIdempotencyKey(UUID operationId);

  /** Open = PENDING + RUNNING + FAILED (still retryable). Low-cardinality gauge source. */
  long countOpenJobs();

  long countExhaustedJobs();
}
