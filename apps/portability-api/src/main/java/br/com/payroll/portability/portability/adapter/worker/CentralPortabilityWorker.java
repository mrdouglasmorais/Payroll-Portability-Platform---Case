package br.com.payroll.portability.portability.adapter.worker;

import br.com.payroll.portability.beneficiary.application.BeneficiaryRepository;
import br.com.payroll.portability.integration.central.CentralException;
import br.com.payroll.portability.integration.central.CentralGateway;
import br.com.payroll.portability.integration.central.CentralPortabilityRequest;
import br.com.payroll.portability.integration.central.CentralProperties;
import br.com.payroll.portability.integration.central.CentralStatus;
import br.com.payroll.portability.integration.central.IntegrationFailureCode;
import br.com.payroll.portability.portability.application.FinalizeOperationService;
import br.com.payroll.portability.portability.application.PortabilityOperationRepository;
import br.com.payroll.portability.portability.application.ProcessingJob;
import br.com.payroll.portability.portability.application.ProcessingJobRepository;
import br.com.payroll.portability.portability.application.StaleFenceException;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.shared.observability.LogEvents;
import br.com.payroll.portability.shared.observability.LogFields;
import br.com.payroll.portability.shared.observability.PortabilityLog;
import br.com.payroll.portability.shared.observability.PortabilityTelemetry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
  private final PortabilityTelemetry telemetry;
  private final ObservationRegistry observationRegistry;
  private final String workerId = "worker-" + UUID.randomUUID();

  public CentralPortabilityWorker(
      ProcessingJobRepository jobRepository,
      PortabilityOperationRepository operationRepository,
      BeneficiaryRepository beneficiaryRepository,
      FinalizeOperationService finalizeOperationService,
      CentralGateway centralGateway,
      CentralProperties properties,
      Clock clock,
      PortabilityTelemetry telemetry,
      ObservationRegistry observationRegistry) {
    this.jobRepository = jobRepository;
    this.operationRepository = operationRepository;
    this.beneficiaryRepository = beneficiaryRepository;
    this.finalizeOperationService = finalizeOperationService;
    this.centralGateway = centralGateway;
    this.properties = properties;
    this.clock = clock;
    this.telemetry = telemetry;
    this.observationRegistry = observationRegistry;
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
        .ifPresent(
            job -> {
              telemetry.jobClaimed();
              processClaimed(job);
            });
  }

  void processClaimed(ProcessingJob job) {
    Observation observation =
        Observation.createNotStarted("portability.job", observationRegistry)
            .lowCardinalityKeyValue("job.type", job.jobType());
    observation.start();
    String outcome = "error";
    try (Observation.Scope scope = observation.openScope()) {
      putJobMdc(job);
      try {
        operationRepository
            .findById(job.operationId())
            .ifPresent(op -> putCorrelationMdc(op.correlationId()));
        if (SUBMIT.equals(job.jobType())) {
          outcome = submit(job);
        } else if (POLL.equals(job.jobType())) {
          outcome = poll(job);
        } else {
          failTransient(
              job,
              "Tipo de job desconhecido: " + job.jobType(),
              IntegrationFailureCode.INTEGRATION_ERROR,
              null);
          outcome = "unknown_type";
        }
      } catch (StaleFenceException ex) {
        telemetry.staleFence();
        PortabilityLog.warn(
            log, LogEvents.JOB_FENCE_STALE, "Stale fence; skipping financial mutation");
        outcome = "stale_fence";
      } catch (CentralException ex) {
        if (ex.isPermanentFailure()) {
          telemetry.centralPermanentFailure();
          failPermanent(
              job,
              "Erro permanente da Central: " + ex.getMessage(),
              IntegrationFailureCode.PERMANENT_CENTRAL_ERROR);
          outcome = "permanent_failure";
        } else {
          telemetry.centralTransientFailure();
          failTransient(
              job,
              sanitizeError(ex.getMessage()),
              IntegrationFailureCode.fromCentral(ex),
              ex.retryAfter());
          outcome = "transient_failure";
        }
      } catch (RuntimeException ex) {
        log.atWarn()
            .addKeyValue(LogFields.EVENT, LogEvents.CENTRAL_CALL_FAILED)
            .setCause(ex)
            .log("Worker failure");
        failTransient(
            job,
            sanitizeError(ex.getMessage()),
            IntegrationFailureCode.INTEGRATION_ERROR,
            null);
        outcome = "runtime_failure";
      } finally {
        clearJobMdc();
      }
    } finally {
      observation.lowCardinalityKeyValue("outcome", outcome);
      observation.stop();
    }
  }

  private String submit(ProcessingJob job) {
    PortabilityOperation operation =
        operationRepository.findById(job.operationId()).orElseThrow();
    if (operation.centralProtocol() != null) {
      reschedulePoll(job, null, null);
      return "already_submitted";
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

    try {
      String protocol =
          centralGateway.submitPortability(request, idempotencyKey, operation.correlationId());

      finalizeOperationService.attachProtocolWithFence(
          operation.id(), protocol, job.id(), job.fenceToken(), owner(job));
      log.atInfo()
          .addKeyValue(LogFields.EVENT, LogEvents.CENTRAL_SUBMIT_ACCEPTED)
          .addKeyValue(LogFields.PROTOCOL, protocol)
          .log("Central accepted submit");
      reschedulePoll(job, null, null);
      return "submitted";
    } catch (CentralException ex) {
      log.atWarn()
          .addKeyValue(LogFields.EVENT, LogEvents.CENTRAL_CALL_FAILED)
          .addKeyValue(LogFields.ERROR_CODE, IntegrationFailureCode.fromCentral(ex).name())
          .addKeyValue(LogFields.ATTEMPT, String.valueOf(job.attemptCount() + 1))
          .log("Central submit failed");
      throw ex;
    }
  }

  private String poll(ProcessingJob job) {
    PortabilityOperation operation =
        operationRepository.findById(job.operationId()).orElseThrow();
    if (operation.businessStatus().isTerminal()) {
      jobRepository.markSucceededIfOwned(
          job.id(), job.fenceToken(), owner(job), clock.instant());
      return "already_terminal";
    }

    Instant now = clock.instant();
    if (operation.createdAt().plus(properties.getMaxPollDuration()).isBefore(now)) {
      failPermanent(
          job,
          "POLL_DEADLINE_EXCEEDED: escalar para reconciliação",
          IntegrationFailureCode.POLL_DEADLINE_EXCEEDED);
      return "poll_deadline";
    }

    String protocol = operation.centralProtocol();
    if (protocol == null || protocol.isBlank()) {
      rescheduleAs(
          job, SUBMIT, "Protocolo ausente; reenviar", null, properties.getPollDelay());
      return "resubmit";
    }

    CentralStatus status;
    try {
      status = centralGateway.pollStatus(protocol, operation.correlationId());
    } catch (CentralException ex) {
      log.atWarn()
          .addKeyValue(LogFields.EVENT, LogEvents.CENTRAL_CALL_FAILED)
          .addKeyValue(LogFields.ERROR_CODE, IntegrationFailureCode.fromCentral(ex).name())
          .addKeyValue(LogFields.ATTEMPT, String.valueOf(job.attemptCount() + 1))
          .addKeyValue(LogFields.PROTOCOL, protocol)
          .log("Central poll failed");
      throw ex;
    }

    if (status.awaiting()) {
      Duration delay =
          status.retryAfter() != null && !status.retryAfter().isZero()
              ? status.retryAfter()
              : properties.getPollDelay();
      log.atInfo()
          .addKeyValue(LogFields.EVENT, LogEvents.CENTRAL_POLL_PENDING)
          .addKeyValue(LogFields.PROTOCOL, protocol)
          .log("Central still pending");
      reschedulePoll(job, null, delay);
      return "pending";
    }

    if ("CONFIRMADA".equals(status.status())) {
      finalizeOperationService.confirmWithFence(
          operation.id(), job.id(), job.fenceToken(), owner(job));
      return "confirmed";
    }
    if ("REJEITADA".equals(status.status())) {
      String reason =
          status.motivoRejeicao() == null || status.motivoRejeicao().isBlank()
              ? "Rejeitada pela Central"
              : status.motivoRejeicao();
      finalizeOperationService.rejectWithFence(
          operation.id(), reason, job.id(), job.fenceToken(), owner(job));
      return "rejected";
    }

    failTransient(
        job,
        "Status inesperado da Central: " + status.status(),
        IntegrationFailureCode.INTEGRATION_ERROR,
        null);
    return "unexpected_status";
  }

  private void reschedulePoll(ProcessingJob job, String error, Duration delayOverride) {
    Duration delay = delayOverride == null ? properties.getPollDelay() : delayOverride;
    rescheduleAs(job, POLL, error, null, delay);
  }

  private void rescheduleAs(
      ProcessingJob job, String jobType, String error, String errorCode, Duration delay) {
    Instant now = clock.instant();
    jobRepository.reschedule(
        job.id(),
        job.fenceToken(),
        jobType,
        now.plus(delay),
        job.attemptCount(),
        error,
        errorCode,
        now);
  }

  private void failTransient(
      ProcessingJob job,
      String error,
      IntegrationFailureCode code,
      Duration retryAfter) {
    Instant now = clock.instant();
    int attempts = job.attemptCount() + 1;
    String sanitized = sanitizeError(error);
    finalizeOperationService.recordIntegrationFailureWithFence(
        job.operationId(), job.id(), job.fenceToken(), owner(job), sanitized);
    if (attempts >= properties.getMaxAttempts()) {
      jobRepository.markExhausted(
          job.id(), job.fenceToken(), sanitized, code.name(), now);
      telemetry.jobExhausted();
      log.atError()
          .addKeyValue(LogFields.EVENT, LogEvents.JOB_EXHAUSTED)
          .addKeyValue(LogFields.ERROR_CODE, code.name())
          .addKeyValue(LogFields.ATTEMPT, String.valueOf(attempts))
          .log("Job exhausted; requires review");
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
        code.name(),
        now);
  }

  private void failPermanent(ProcessingJob job, String error, IntegrationFailureCode code) {
    Instant now = clock.instant();
    String sanitized = sanitizeError(error);
    finalizeOperationService.recordIntegrationFailureWithFence(
        job.operationId(), job.id(), job.fenceToken(), owner(job), sanitized);
    jobRepository.markExhausted(job.id(), job.fenceToken(), sanitized, code.name(), now);
    telemetry.jobExhausted();
    log.atError()
        .addKeyValue(LogFields.EVENT, LogEvents.JOB_EXHAUSTED)
        .addKeyValue(LogFields.ERROR_CODE, code.name())
        .log("Job permanently exhausted; requires review");
  }

  private static void putJobMdc(ProcessingJob job) {
    MDC.put(LogFields.OPERATION_ID, job.operationId().toString());
    MDC.put(LogFields.JOB_ID, job.id().toString());
    MDC.put(LogFields.ATTEMPT, String.valueOf(job.attemptCount()));
    MDC.put(LogFields.FENCE_TOKEN, String.valueOf(job.fenceToken()));
    MDC.put(LogFields.JOB_TYPE, job.jobType());
  }

  private static void putCorrelationMdc(String correlationId) {
    if (correlationId != null && !correlationId.isBlank()) {
      MDC.put(LogFields.CORRELATION_ID, correlationId);
    }
  }

  private static void clearJobMdc() {
    MDC.remove(LogFields.OPERATION_ID);
    MDC.remove(LogFields.JOB_ID);
    MDC.remove(LogFields.CORRELATION_ID);
    MDC.remove(LogFields.ATTEMPT);
    MDC.remove(LogFields.FENCE_TOKEN);
    MDC.remove(LogFields.JOB_TYPE);
  }

  private static String owner(ProcessingJob job) {
    return job.leaseOwner() == null ? "" : job.leaseOwner();
  }

  private static String sanitizeError(String error) {
    if (error == null || error.isBlank()) {
      return "erro desconhecido";
    }
    String trimmed = error.replaceAll("[\\r\\n]+", " ").trim();
    return trimmed.length() > 240 ? trimmed.substring(0, 240) : trimmed;
  }
}
