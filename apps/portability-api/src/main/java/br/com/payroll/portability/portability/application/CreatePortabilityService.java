package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.consent.application.ConsentRepository;
import br.com.payroll.portability.portability.domain.PortabilityCommand;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import br.com.payroll.portability.schedule.domain.InstallmentStatus;
import br.com.payroll.portability.shared.domain.DomainException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePortabilityService {

  public static final String IDEMPOTENCY_SCOPE = "portability-create";

  private final ConsentRepository consentRepository;
  private final InstallmentRepository installmentRepository;
  private final PortabilityOperationRepository operationRepository;
  private final Clock clock;

  public CreatePortabilityService(
      ConsentRepository consentRepository,
      InstallmentRepository installmentRepository,
      PortabilityOperationRepository operationRepository,
      Clock clock) {
    this.consentRepository = consentRepository;
    this.installmentRepository = installmentRepository;
    this.operationRepository = operationRepository;
    this.clock = clock;
  }

  @Transactional
  public UUID create(PortabilityCommand command) {
    var existingId =
        operationRepository.findOperationIdByIdempotency(IDEMPOTENCY_SCOPE, command.idempotencyKey());
    if (existingId.isPresent()) {
      String storedHash =
          operationRepository
              .findRequestHashByIdempotency(IDEMPOTENCY_SCOPE, command.idempotencyKey())
              .orElseThrow();
      if (!storedHash.equals(command.requestHash())) {
        throw new IdempotencyConflictException(
            "Idempotency key reused with a different payload");
      }
      return existingId.get();
    }

    if (!consentRepository.hasAcceptedConsent(command.beneficiaryId())) {
      throw new DomainException("Beneficiary has no accepted consent");
    }

    List<InstallmentSnapshot> installments =
        installmentRepository.findByExternalIds(command.externalInstallmentIds());
    if (installments.size() != command.externalInstallmentIds().size()) {
      throw new DomainException("One or more installments were not found");
    }

    Set<String> agreements = new HashSet<>();
    for (InstallmentSnapshot installment : installments) {
      if (!installment.beneficiaryId().equals(command.beneficiaryId())) {
        throw new DomainException("Installment does not belong to beneficiary");
      }
      if (installment.status() != InstallmentStatus.OPEN) {
        throw new DomainException("Installment is not open: " + installment.externalInstallmentId());
      }
      agreements.add(installment.agreementId());
      if (consentRepository.findAccepted(command.beneficiaryId(), installment.agreementId()).isEmpty()) {
        throw new DomainException("No accepted consent for agreement " + installment.agreementId());
      }
    }

    Instant now = clock.instant();
    PortabilityOperation operation =
        PortabilityOperation.create(
            command.beneficiaryId(),
            command.assigneeCreditor(),
            command.receivingAccount(),
            command.externalInstallmentIds(),
            command.correlationId(),
            now);

    for (InstallmentSnapshot installment : installments) {
      installment.reserve();
      installmentRepository.save(installment);
    }

    operationRepository.insertAtomicCreate(
        operation, IDEMPOTENCY_SCOPE, command.idempotencyKey(), command.requestHash());

    return operation.id();
  }
}
