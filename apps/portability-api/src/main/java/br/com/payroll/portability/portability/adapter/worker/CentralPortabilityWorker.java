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
import br.com.payroll.portability.portability.application.StaleFenceException;
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

  String workerId() {
    return workerId;
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
        failTransient(job, "Unknown job type: " + job.jobType(), null);
      }
    } catch (StaleFenceException ex) {
      log.info("Stale fence on job {}; skipping financial mutation", job.id());
    } catch (CentralException ex) {
      if (ex.isPermanentFailure()) {
        failPermanent(job, "Permanent central error: " + ex.getMessage());
      } else {
        failTransient(job, sanitizeError(ex.getMessage()), ex.retryAfter());
      }
    } catch (RuntimeException ex) {
      log.warn("Worker failure on job {}", job.id(), ex);
      failTransient(job, sanitizeError(ex.getMessage()), null);
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

    finalizeOperationService.attachProtocolWithFence(
        operation.id(), protocol, job.id(), job.fenceToken(), owner(job));
    reschedulePoll(job, null);
  }

  private void poll(ProcessingJob job) {
    PortabilityOperation operation =
        operationRepository.findById(job.operationId()).orElseThrow();
    if (operation.businessStatus().isTerminal()) {
      jobRepository.markSucceededIfOwned(
          job.id(), job.fenceToken(), owner(job), clock.instant());
      return;
    }

    Instant now = clock.instant();
    if (operation
        .createdAt()
        .plus(properties.getMaxPollDuration())
        .isBefore(now)) {
      failPermanent(job, "POLL_DEADLINE_EXCEEDED: escalate to reconciliation");
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
      finalizeOperationService.confirmWithFence(
          operation.id(), job.id(), job.fenceToken(), owner(job));
      return;
    }
    if ("REJEITADA".equals(status.status())) {
      String reason =
          status.motivoRejeicao() == null || status.motivoRejeicao().isBlank()
              ? "Rejected by Central"
              : status.motivoRejeicao();
      finalizeOperationService.rejectWithFence(
          operation.id(), reason, job.id(), job.fenceToken(), owner(job));
      return;
    }

    failTransient(job, "Unexpected central status: " + status.status(), null);
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

  private void failTransient(ProcessingJob job, String error, Duration retryAfter) {
    Instant now = clock.instant();
    int attempts = job.attemptCount() + 1;
    String sanitized = sanitizeError(error);
    finalizeOperationService.recordIntegrationFailure(job.operationId(), sanitized);
    if (attempts >= properties.getMaxAttempts()) {
      jobRepository.markExhausted(job.id(), job.fenceToken(), sanitized, now);
      return;
    }
    Duration backoff =
        retryAfter != null && !retryAfter.isNegative() && !retryAfter.isZero()
            ? retryAfter
            : JobBackoff.nextDelay(
                properties.getPollDelay(), attempts, properties.getMaxBackoff());
    jobRepository.reschedule(
        job.id(),
        job.fenceToken(),
        job.jobType(),
        now.plus(backoff),
        attempts,
        sanitized,
        now);
  }

  private void failPermanent(ProcessingJob job, String error) {
    Instant now = clock.instant();
    String sanitized = sanitizeError(error);
    finalizeOperationService.recordIntegrationFailure(job.operationId(), sanitized);
    jobRepository.markExhausted(job.id(), job.fenceToken(), sanitized, now);
  }

  private static String owner(ProcessingJob job) {
    return job.leaseOwner() == null ? "" : job.leaseOwner();
  }

  private static String sanitizeError(String error) {
    if (error == null || error.isBlank()) {
      return "unknown error";
    }
    String trimmed = error.replaceAll("[\\r\\n]+", " ").trim();
    return trimmed.length() > 240 ? trimmed.substring(0, 240) : trimmed;
  }
}
