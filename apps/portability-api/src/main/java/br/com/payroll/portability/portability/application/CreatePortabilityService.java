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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    Optional<UUID> claimedOrExisting =
        operationRepository.claimIdempotencyOrFind(
            IDEMPOTENCY_SCOPE, command.idempotencyKey(), command.requestHash());
    if (claimedOrExisting.isPresent()) {
      return claimedOrExisting.get();
    }

    boolean claimOwned = true;
    try {
      if (!consentRepository.hasAcceptedConsent(command.beneficiaryId())) {
        throw new DomainException("Beneficiário sem consentimento aceito");
      }

      List<InstallmentSnapshot> installments =
          installmentRepository.findByExternalIds(command.externalInstallmentIds()).stream()
              .sorted(Comparator.comparing(InstallmentSnapshot::externalInstallmentId))
              .toList();
      if (installments.size() != command.externalInstallmentIds().size()) {
        throw new DomainException("Uma ou mais parcelas não foram encontradas");
      }

      LocalDate today = LocalDate.ofInstant(clock.instant(), clock.getZone());
      for (InstallmentSnapshot installment : installments) {
        if (!installment.beneficiaryId().equals(command.beneficiaryId())) {
          throw new DomainException("Parcela não pertence ao beneficiário");
        }
        if (installment.status() != InstallmentStatus.OPEN) {
          throw new DomainException(
              "Parcela não está aberta: " + installment.externalInstallmentId());
        }
        if (installment.discountDate().isBefore(today)) {
          throw new DomainException(
              "Data de desconto da parcela não é elegível: "
                  + installment.externalInstallmentId());
        }
        if (consentRepository
            .findAccepted(command.beneficiaryId(), installment.agreementId())
            .isEmpty()) {
          throw new DomainException(
              "Não há consentimento aceito para o convênio " + installment.agreementId());
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

      operationRepository.insertAtomicCreate(operation);
      operationRepository.bindIdempotencyOperation(
          IDEMPOTENCY_SCOPE, command.idempotencyKey(), operation.id());
      claimOwned = false;
      return operation.id();
    } finally {
      if (claimOwned) {
        operationRepository.releaseIdempotencyClaim(
            IDEMPOTENCY_SCOPE, command.idempotencyKey());
      }
    }
  }
}
