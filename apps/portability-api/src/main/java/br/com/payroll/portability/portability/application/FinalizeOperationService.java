package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizeOperationService {

  private final PortabilityOperationRepository operationRepository;
  private final InstallmentRepository installmentRepository;
  private final Clock clock;

  public FinalizeOperationService(
      PortabilityOperationRepository operationRepository,
      InstallmentRepository installmentRepository,
      Clock clock) {
    this.operationRepository = operationRepository;
    this.installmentRepository = installmentRepository;
    this.clock = clock;
  }

  @Transactional
  public void attachProtocol(UUID operationId, String protocol) {
    PortabilityOperation operation =
        operationRepository.findById(operationId).orElseThrow();
    Instant now = clock.instant();
    operation.attachProtocol(protocol, now);
    operationRepository.saveOutcome(operation);
  }

  @Transactional
  public void confirm(UUID operationId) {
    PortabilityOperation operation =
        operationRepository.findById(operationId).orElseThrow();
    Instant now = clock.instant();
    operation.confirm(now);
    for (String externalId : operation.externalInstallmentIds()) {
      InstallmentSnapshot installment =
          installmentRepository.findByExternalId(externalId).orElseThrow();
      installment.markPorted();
      installmentRepository.save(installment);
      operationRepository.consumeReservation(operationId, externalId, now);
    }
    operationRepository.saveOutcome(operation);
  }

  @Transactional
  public void reject(UUID operationId, String reason) {
    PortabilityOperation operation =
        operationRepository.findById(operationId).orElseThrow();
    Instant now = clock.instant();
    operation.reject(reason, now);
    for (String externalId : operation.externalInstallmentIds()) {
      InstallmentSnapshot installment =
          installmentRepository.findByExternalId(externalId).orElseThrow();
      installment.release();
      installmentRepository.save(installment);
      operationRepository.releaseReservation(operationId, externalId, now);
    }
    operationRepository.saveOutcome(operation);
  }
}
