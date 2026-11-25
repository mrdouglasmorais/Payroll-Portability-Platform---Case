package br.com.payroll.portability.portability.adapter.worker;

import br.com.payroll.portability.beneficiary.application.BeneficiaryRepository;
import br.com.payroll.portability.integration.central.CentralException;
import br.com.payroll.portability.integration.central.CentralGateway;
import br.com.payroll.portability.integration.central.CentralPortabilityRequest;
import br.com.payroll.portability.integration.central.CentralProperties;
import br.com.payroll.portability.integration.central.CentralStatus;
import br.com.payroll.portability.portability.application.FinalizeOperationService;
import br.com.payroll.portability.portability.application.PortabilityOperationRepository;
import br.com.payroll.portability.portability.application.ProcessingJob;
import br.com.payroll.portability.portability.application.ProcessingJobRepository;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CentralPortabilityWorker {

  public static final String SUBMIT = "SUBMIT_TO_CENTRAL";
  public static final String POLL = "POLL_CENTRAL";

  private static final Logger log = LoggerFactory.getLogger(CentralPortabilityWorker.class);

  private final ProcessingJobRepository jobRepository;
  private final PortabilityOperationRepository operationRepository;
  private final BeneficiaryRepository beneficiaryRepository;
  private final FinalizeOperationService finalizeOperationService;
  private final CentralGateway centralGateway;
  private final CentralProperties properties;
  private final Clock clock;
  private final String workerId = "worker-" + UUID.randomUUID();

  public CentralPortabilityWorker(
      ProcessingJobRepository jobRepository,
      PortabilityOperationRepository operationRepository,
      BeneficiaryRepository beneficiaryRepository,
      FinalizeOperationService finalizeOperationService,
      CentralGateway centralGateway,
      CentralProperties properties,
      Clock clock) {
    this.jobRepository = jobRepository;
    this.operationRepository = operationRepository;
    this.beneficiaryRepository = beneficiaryRepository;
    this.finalizeOperationService = finalizeOperationService;
    this.centralGateway = centralGateway;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${app.central.worker-tick:PT1S}")
  public void tick() {
    if (!properties.isWorkerEnabled()) {
      return;
    }
    Instant now = clock.instant();
    Instant leaseUntil = now.plus(properties.getLeaseDuration());
    jobRepository
        .claimNext(workerId, now, leaseUntil)
        .ifPresent(this::processClaimed);
  }

  void processClaimed(ProcessingJob job) {
    try {
      if (SUBMIT.equals(job.jobType())) {
        submit(job);
      } else if (POLL.equals(job.jobType())) {
        poll(job);
      } else {
        failTransient(job, "Unknown job type: " + job.jobType());
      }
    } catch (CentralException ex) {
      if (ex.isTransientFailure()) {
        failTransient(job, ex.getMessage());
      } else {
        failTransient(job, "Non-retryable central error held for review: " + ex.getMessage());
      }
    } catch (RuntimeException ex) {
      log.warn("Worker failure on job {}", job.id(), ex);
      failTransient(job, ex.getMessage());
    }
  }

  private void submit(ProcessingJob job) {
    PortabilityOperation operation =
        operationRepository.findById(job.operationId()).orElseThrow();
    if (operation.centralProtocol() != null) {
      reschedulePoll(job, null);
      return;
    }

    String externalRef =
        beneficiaryRepository
            .findExternalRefById(operation.beneficiaryId())
            .orElseThrow();
    String idempotencyKey =
        jobRepository.findIdempotencyKey(operation.id()).orElseThrow();

    CentralPortabilityRequest request =
        new CentralPortabilityRequest(
            externalRef,
            new ArrayList<>(operation.externalInstallmentIds()),
            operation.assigneeCreditor(),
            operation.receivingAccount().bank(),
            operation.receivingAccount().agency(),
            operation.receivingAccount().account());

    // HTTP outside DB transaction
    String protocol =
        centralGateway.submitPortability(request, idempotencyKey, operation.correlationId());

    finalizeOperationService.attachProtocol(operation.id(), protocol);
    reschedulePoll(job, null);
  }

  private void poll(ProcessingJob job) {
    PortabilityOperation operation =
        operationRepository.findById(job.operationId()).orElseThrow();
    if (operation.businessStatus().isTerminal()) {
      jobRepository.markSucceeded(job.id(), job.fenceToken(), clock.instant());
      return;
    }
    String protocol = operation.centralProtocol();
    if (protocol == null || protocol.isBlank()) {
      rescheduleAs(job, SUBMIT, "Missing protocol; re-submit", properties.getPollDelay());
      return;
    }

    CentralStatus status = centralGateway.pollStatus(protocol, operation.correlationId());
    if (status.awaiting()) {
      reschedulePoll(job, null);
      return;
    }

    if ("CONFIRMADA".equals(status.status())) {
      finalizeOperationService.confirm(operation.id());
      jobRepository.markSucceeded(job.id(), job.fenceToken(), clock.instant());
      return;
    }
    if ("REJEITADA".equals(status.status())) {
      String reason =
          status.motivoRejeicao() == null || status.motivoRejeicao().isBlank()
              ? "Rejected by Central"
              : status.motivoRejeicao();
      finalizeOperationService.reject(operation.id(), reason);
      jobRepository.markSucceeded(job.id(), job.fenceToken(), clock.instant());
      return;
    }

    failTransient(job, "Unexpected central status: " + status.status());
  }

  private void reschedulePoll(ProcessingJob job, String error) {
    rescheduleAs(job, POLL, error, properties.getPollDelay());
  }

  private void rescheduleAs(ProcessingJob job, String jobType, String error, Duration delay) {
    Instant now = clock.instant();
    jobRepository.reschedule(
        job.id(),
        job.fenceToken(),
        jobType,
        now.plus(delay),
        job.attemptCount(),
        error,
        now);
  }

  private void failTransient(ProcessingJob job, String error) {
    Instant now = clock.instant();
    int attempts = job.attemptCount() + 1;
    if (attempts >= properties.getMaxAttempts()) {
      jobRepository.markExhausted(job.id(), job.fenceToken(), error, now);
      return;
    }
    Duration backoff =
        properties.getPollDelay().multipliedBy(Math.min(attempts, 8));
    jobRepository.reschedule(
        job.id(),
        job.fenceToken(),
        job.jobType(),
        now.plus(backoff),
        attempts,
        error,
        now);
  }
}
