package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.consent.application.ConsentRepository;
import br.com.payroll.portability.portability.domain.PortabilityCommand;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import br.com.payroll.portability.schedule.domain.InstallmentStatus;
import br.com.payroll.portability.shared.domain.DomainException;
import br.com.payroll.portability.shared.domain.InstallmentConflictException;
import br.com.payroll.portability.shared.observability.LogEvents;
import br.com.payroll.portability.shared.observability.LogFields;
import br.com.payroll.portability.shared.observability.PortabilityTelemetry;
import br.com.payroll.portability.shared.observability.SensitiveData;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatePortabilityService {

  public static final String IDEMPOTENCY_SCOPE = "portability-create";

  private static final Logger log = LoggerFactory.getLogger(CreatePortabilityService.class);

  private final ConsentRepository consentRepository;
  private final InstallmentRepository installmentRepository;
  private final PortabilityOperationRepository operationRepository;
  private final Clock clock;
  private final PortabilityTelemetry telemetry;

  public CreatePortabilityService(
      ConsentRepository consentRepository,
      InstallmentRepository installmentRepository,
      PortabilityOperationRepository operationRepository,
      Clock clock,
      PortabilityTelemetry telemetry) {
    this.consentRepository = consentRepository;
    this.installmentRepository = installmentRepository;
    this.operationRepository = operationRepository;
    this.clock = clock;
    this.telemetry = telemetry;
  }

  @Transactional
  public UUID create(PortabilityCommand command) {
    Optional<UUID> claimedOrExisting;
    try {
      claimedOrExisting =
          operationRepository.claimIdempotencyOrFind(
              IDEMPOTENCY_SCOPE, command.idempotencyKey(), command.requestHash());
    } catch (IdempotencyConflictException ex) {
      // correlationId lives in MDC — do not addKeyValue (ECS duplicate key).
      log.atWarn()
          .addKeyValue(LogFields.EVENT, LogEvents.IDEMPOTENCY_CONFLICT)
          .log("Idempotency key reused with different payload");
      throw ex;
    }
    if (claimedOrExisting.isPresent()) {
      log.atInfo()
          .addKeyValue(LogFields.EVENT, LogEvents.IDEMPOTENCY_REPLAY)
          .addKeyValue(LogFields.OPERATION_ID, claimedOrExisting.get().toString())
          .log("Idempotent replay of create");
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
          throw unavailableInstallment(installment.externalInstallmentId(), installment.status());
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

      // Conditional UPDATE prevents lost races; loser maps to HTTP 409 (never 500).
      for (InstallmentSnapshot installment : installments) {
        if (installmentRepository.reserveIfOpen(installment.id()) != 1) {
          InstallmentStatus current =
              installmentRepository
                  .findByExternalId(installment.externalInstallmentId())
                  .map(InstallmentSnapshot::status)
                  .orElse(InstallmentStatus.RESERVED);
          throw unavailableInstallment(installment.externalInstallmentId(), current);
        }
      }

      operationRepository.insertAtomicCreate(operation);
      operationRepository.bindIdempotencyOperation(
          IDEMPOTENCY_SCOPE, command.idempotencyKey(), operation.id());
      claimOwned = false;
      telemetry.operationCreated();
      log.atInfo()
          .addKeyValue(LogFields.EVENT, LogEvents.PORTABILITY_CREATED)
          .addKeyValue(LogFields.OPERATION_ID, operation.id().toString())
          .addKeyValue(LogFields.RECEIVING_BANK, command.receivingAccount().bank())
          .addKeyValue(
              LogFields.RECEIVING_AGENCY_MASKED,
              SensitiveData.maskAgency(command.receivingAccount().agency()))
          .addKeyValue(
              LogFields.RECEIVING_ACCOUNT_MASKED,
              SensitiveData.maskAccount(command.receivingAccount().account()))
          .log("Portability operation created");
      return operation.id();
    } catch (InstallmentConflictException ex) {
      log.atWarn()
          .addKeyValue(LogFields.EVENT, LogEvents.RESERVATION_CONFLICT)
          .addKeyValue(LogFields.PARCELA_ID, ex.externalInstallmentId())
          .log("Installment reservation conflict");
      throw ex;
    } finally {
      if (claimOwned) {
        operationRepository.releaseIdempotencyClaim(
            IDEMPOTENCY_SCOPE, command.idempotencyKey());
      }
    }
  }

  private static InstallmentConflictException unavailableInstallment(
      String externalId, InstallmentStatus status) {
    if (status == InstallmentStatus.PORTED) {
      return InstallmentConflictException.alreadyPorted(externalId);
    }
    if (status == InstallmentStatus.RESERVED) {
      return InstallmentConflictException.alreadyReserved(externalId);
    }
    return InstallmentConflictException.unavailable(externalId);
  }
}
