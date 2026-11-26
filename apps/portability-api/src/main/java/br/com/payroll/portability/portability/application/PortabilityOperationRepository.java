package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.portability.domain.BusinessStatus;
import br.com.payroll.portability.portability.domain.OperationEvent;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortabilityOperationRepository {

  /**
   * Atomically claims an idempotency key (operation_id null). Returns empty when this caller
   * owns the claim; returns existing operation id on replay; throws on hash mismatch.
   */
  Optional<UUID> claimIdempotencyOrFind(
      String scope, String key, String requestHash);

  void bindIdempotencyOperation(String scope, String key, UUID operationId);

  void releaseIdempotencyClaim(String scope, String key);

  void insertAtomicCreate(PortabilityOperation operation);

  Optional<UUID> findOperationIdByIdempotency(String scope, String key);

  Optional<String> findRequestHashByIdempotency(String scope, String key);

  Optional<PortabilityOperation> findById(UUID operationId);

  /**
   * Paginated operations for a beneficiary external ref, newest first. Empty page when unknown
   * beneficiary.
   */
  OperationPage pageByBeneficiaryExternalRef(String externalRef, int page, int size);

  void saveOutcome(PortabilityOperation operation);

  /** Optimistic transition: updates only while previous business status matches. */
  int saveOutcomeIfPending(PortabilityOperation operation, BusinessStatus expectedStatus);

  /**
   * Persists new technical events without changing business_status; no-op (0) if terminal.
   */
  int appendEventsIfNonTerminal(PortabilityOperation operation);

  void consumeReservation(UUID operationId, String externalInstallmentId, Instant now);

  void releaseReservation(UUID operationId, String externalInstallmentId, Instant now);

  List<OperationEvent> listEvents(UUID operationId);
}
