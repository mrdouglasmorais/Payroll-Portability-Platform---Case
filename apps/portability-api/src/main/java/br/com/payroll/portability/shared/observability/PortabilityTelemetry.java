package br.com.payroll.portability.shared.observability;

import br.com.payroll.portability.portability.application.ProcessingJobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Low-cardinality metrics for portability operations and jobs. Never labels with
 * beneficiary/operation IDs.
 *
 * <p>Financial outcome counters ({@code created}/{@code confirmed}/{@code rejected}) are scheduled
 * with {@code afterCommit} so a rolled-back transaction does not inflate the count. These metrics
 * are operational signals only — not an authoritative accounting source. The database and append-only
 * audit trail remain the source of financial truth.
 */
@Component
public class PortabilityTelemetry {

  private final MeterRegistry registry;
  private final ProcessingJobRepository jobRepository;

  private Counter operationsCreated;
  private Counter operationsConfirmed;
  private Counter operationsRejected;
  private Counter jobsClaimed;
  private Counter jobsStaleFence;
  private Counter jobsExhausted;
  private Counter centralTransient;
  private Counter centralPermanent;

  public PortabilityTelemetry(MeterRegistry registry, ProcessingJobRepository jobRepository) {
    this.registry = registry;
    this.jobRepository = jobRepository;
  }

  @PostConstruct
  void bind() {
    operationsCreated =
        Counter.builder("portability_operations_total")
            .description("Portability operations created (after successful commit)")
            .tag("outcome", "created")
            .register(registry);
    operationsConfirmed =
        Counter.builder("portability_operations_total")
            .description("Portability operations finalized (after successful commit)")
            .tag("outcome", "confirmed")
            .register(registry);
    operationsRejected =
        Counter.builder("portability_operations_total")
            .description("Portability operations finalized (after successful commit)")
            .tag("outcome", "rejected")
            .register(registry);
    jobsClaimed =
        Counter.builder("portability_jobs_claimed_total")
            .description("Worker job claims")
            .register(registry);
    jobsStaleFence =
        Counter.builder("portability_jobs_stale_fence_total")
            .description("Stale fence detections")
            .register(registry);
    jobsExhausted =
        Counter.builder("portability_jobs_exhausted_total")
            .description("Jobs marked EXHAUSTED / REQUIRES_REVIEW")
            .register(registry);
    centralTransient =
        Counter.builder("portability_central_failures_total")
            .tag("class", "transient")
            .register(registry);
    centralPermanent =
        Counter.builder("portability_central_failures_total")
            .tag("class", "permanent")
            .register(registry);

    registry.gauge(
        "portability_jobs_backlog",
        Tags.of("state", "open"),
        jobRepository,
        ProcessingJobRepository::countOpenJobs);
    registry.gauge(
        "portability_jobs_backlog",
        Tags.of("state", "exhausted"),
        jobRepository,
        ProcessingJobRepository::countExhaustedJobs);
  }

  public void operationCreated() {
    afterCommit(operationsCreated::increment);
  }

  public void operationConfirmed() {
    afterCommit(operationsConfirmed::increment);
  }

  public void operationRejected() {
    afterCommit(operationsRejected::increment);
  }

  public void jobClaimed() {
    jobsClaimed.increment();
  }

  public void staleFence() {
    jobsStaleFence.increment();
  }

  public void jobExhausted() {
    jobsExhausted.increment();
  }

  public void centralTransientFailure() {
    centralTransient.increment();
  }

  public void centralPermanentFailure() {
    centralPermanent.increment();
  }

  /**
   * Runs {@code action} after a successful commit when a Spring transaction synchronization is
   * active; otherwise runs immediately (non-transactional call sites).
   */
  void afterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
      return;
    }
    action.run();
  }
}
