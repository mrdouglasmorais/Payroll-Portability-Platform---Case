package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.portability.domain.BusinessStatus;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
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

  public FinalizeOperationService(
      PortabilityOperationRepository operationRepository,
      InstallmentRepository installmentRepository,
      ProcessingJobRepository jobRepository,
      Clock clock) {
    this.operationRepository = operationRepository;
    this.installmentRepository = installmentRepository;
    this.jobRepository = jobRepository;
    this.clock = clock;
  }

  /**
   * Attach protocol only if the claiming worker still owns the fence; otherwise no-op for stale
   * workers.
   */
  @Transactional
  public boolean attachProtocolWithFence(
      UUID operationId, String protocol, UUID jobId, long fenceToken, String workerId) {
    requireFence(jobId, fenceToken, workerId);
    PortabilityOperation operation =
        operationRepository.findById(operationId).orElseThrow();
    if (operation.centralProtocol() != null) {
      return true;
    }
    Instant now = clock.instant();
    operation.attachProtocol(protocol, now);
    operationRepository.saveOutcome(operation);
    return true;
  }

  @Transactional
  public boolean confirmWithFence(
      UUID operationId, UUID jobId, long fenceToken, String workerId) {
    requireFence(jobId, fenceToken, workerId);
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
    return jobRepository.markSucceededIfOwned(jobId, fenceToken, workerId, now) == 1;
  }

  @Transactional
  public boolean rejectWithFence(
      UUID operationId, String reason, UUID jobId, long fenceToken, String workerId) {
    requireFence(jobId, fenceToken, workerId);
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
    return jobRepository.markSucceededIfOwned(jobId, fenceToken, workerId, now) == 1;
  }

  @Transactional
  public void recordIntegrationFailure(UUID operationId, String sanitizedDescription) {
    PortabilityOperation operation =
        operationRepository.findById(operationId).orElseThrow();
    Instant now = clock.instant();
    operation.recordTechnicalEvent("INTEGRATION_FAILURE", sanitizedDescription, now);
    operationRepository.saveOutcome(operation);
  }

  private void requireFence(UUID jobId, long fenceToken, String workerId) {
    if (!jobRepository.stillOwns(jobId, fenceToken, workerId)) {
      throw new StaleFenceException("Job fence no longer owned by worker");
    }
  }
}
