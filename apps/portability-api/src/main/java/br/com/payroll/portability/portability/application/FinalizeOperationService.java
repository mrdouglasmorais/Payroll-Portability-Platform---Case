package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.portability.domain.BusinessStatus;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import br.com.payroll.portability.shared.observability.PortabilityTelemetry;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizeOperationService {

  private final PortabilityOperationRepository operationRepository;
  private final InstallmentRepository installmentRepository;
  private final ProcessingJobRepository jobRepository;
  private final Clock clock;
  private final PortabilityTelemetry telemetry;

  public FinalizeOperationService(
      PortabilityOperationRepository operationRepository,
      InstallmentRepository installmentRepository,
      ProcessingJobRepository jobRepository,
      Clock clock,
      PortabilityTelemetry telemetry) {
    this.operationRepository = operationRepository;
    this.installmentRepository = installmentRepository;
    this.jobRepository = jobRepository;
    this.clock = clock;
    this.telemetry = telemetry;
  }

  @Transactional
  public boolean attachProtocolWithFence(
      UUID operationId, String protocol, UUID jobId, long fenceToken, String workerId) {
    lockFenceOrThrow(jobId, fenceToken, workerId);
    PortabilityOperation operation =
        operationRepository.findById(operationId).orElseThrow();
    if (operation.centralProtocol() != null) {
      return true;
    }
    Instant now = clock.instant();
    operation.attachProtocol(protocol, now);
    operationRepository.saveOutcome(operation);
    if (!jobRepository.stillOwns(jobId, fenceToken, workerId)) {
      throw new StaleFenceException("Fence lost after protocol attach");
    }
    return true;
  }

  @Transactional
  public boolean confirmWithFence(
      UUID operationId, UUID jobId, long fenceToken, String workerId) {
    lockFenceOrThrow(jobId, fenceToken, workerId);
    PortabilityOperation operation =
        operationRepository.findById(operationId).orElseThrow();
    if (operation.businessStatus().isTerminal()) {
      return jobRepository.markSucceededIfOwned(jobId, fenceToken, workerId, clock.instant()) == 1;
    }
    Instant now = clock.instant();
    operation.confirm(now);
    for (String externalId : operation.externalInstallmentIds()) {
      InstallmentSnapshot installment =
          installmentRepository.findByExternalId(externalId).orElseThrow();
      installment.markPorted(operation.receivingAccount());
      installmentRepository.save(installment);
      operationRepository.consumeReservation(operationId, externalId, now);
    }
    int updated =
        operationRepository.saveOutcomeIfPending(operation, BusinessStatus.PENDENTE);
    if (updated == 0) {
      throw new StaleFenceException("Operation left PENDENTE concurrently");
    }
    if (jobRepository.markSucceededIfOwned(jobId, fenceToken, workerId, now) != 1) {
      throw new StaleFenceException("Fence lost after financial confirm");
    }
    telemetry.operationConfirmed();
    return true;
  }

  @Transactional
  public boolean rejectWithFence(
      UUID operationId, String reason, UUID jobId, long fenceToken, String workerId) {
    lockFenceOrThrow(jobId, fenceToken, workerId);
    PortabilityOperation operation =
        operationRepository.findById(operationId).orElseThrow();
    if (operation.businessStatus().isTerminal()) {
      return jobRepository.markSucceededIfOwned(jobId, fenceToken, workerId, clock.instant()) == 1;
    }
    Instant now = clock.instant();
    List<ReceivingAccount> originalAccounts = new ArrayList<>();
    for (String externalId : operation.externalInstallmentIds()) {
      InstallmentSnapshot installment =
          installmentRepository.findByExternalId(externalId).orElseThrow();
      originalAccounts.add(installment.effectiveAccount());
    }

    operation.reject(reason, now);
    int idx = 0;
    for (String externalId : operation.externalInstallmentIds()) {
      InstallmentSnapshot installment =
          installmentRepository.findByExternalId(externalId).orElseThrow();
      installment.release();
      installmentRepository.save(installment);
      operationRepository.releaseReservation(operationId, externalId, now);
      InstallmentSnapshot after =
          installmentRepository.findByExternalId(externalId).orElseThrow();
      ReceivingAccount original = originalAccounts.get(idx++);
      ReceivingAccount effective = after.effectiveAccount();
      if (!original.bank().equals(effective.bank())
          || !original.agency().equals(effective.agency())
          || !original.account().equals(effective.account())) {
        throw new IllegalStateException("Effective account must not change on rejection");
      }
    }
    int updated =
        operationRepository.saveOutcomeIfPending(operation, BusinessStatus.PENDENTE);
    if (updated == 0) {
      throw new StaleFenceException("Operation left PENDENTE concurrently");
    }
    if (jobRepository.markSucceededIfOwned(jobId, fenceToken, workerId, now) != 1) {
      throw new StaleFenceException("Fence lost after financial reject");
    }
    telemetry.operationRejected();
    return true;
  }

  /**
   * Appends a sanitized integration failure only while the fence is owned and the operation is
   * still non-terminal — never regresses financial status.
   */
  @Transactional
  public boolean recordIntegrationFailureWithFence(
      UUID operationId,
      UUID jobId,
      long fenceToken,
      String workerId,
      String sanitizedDescription) {
    if (!jobRepository.lockOwnedFence(jobId, fenceToken, workerId)) {
      return false;
    }
    PortabilityOperation operation =
        operationRepository.findById(operationId).orElseThrow();
    if (operation.businessStatus().isTerminal()) {
      return false;
    }
    Instant now = clock.instant();
    operation.recordTechnicalEvent("INTEGRATION_FAILURE", sanitizedDescription, now);
    return operationRepository.appendEventsIfNonTerminal(operation) == 1;
  }

  private void lockFenceOrThrow(UUID jobId, long fenceToken, String workerId) {
    if (!jobRepository.lockOwnedFence(jobId, fenceToken, workerId)) {
      throw new StaleFenceException("Job fence no longer owned by worker");
    }
  }
}
