package br.com.payroll.portability.portability.adapter.persistence;

import br.com.payroll.portability.portability.application.PortabilityOperationRepository;
import br.com.payroll.portability.portability.domain.BusinessStatus;
import br.com.payroll.portability.portability.domain.OperationEvent;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import br.com.payroll.portability.portability.domain.TechnicalJobStatus;
import br.com.payroll.portability.shared.domain.InstallmentConflictException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcPortabilityOperationRepository implements PortabilityOperationRepository {

  private final JdbcTemplate jdbc;

  public JdbcPortabilityOperationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<UUID> claimIdempotencyOrFind(
      String scope, String key, String requestHash) {
    int inserted =
        jdbc.update(
            """
            INSERT INTO idempotency_keys (
              id, scope, idempotency_key, request_hash, operation_id, created_at
            ) VALUES (?, ?, ?, ?, NULL, ?)
            ON CONFLICT (scope, idempotency_key) DO NOTHING
            """,
            UUID.randomUUID(),
            scope,
            key,
            requestHash,
            Timestamp.from(Instant.now()));
    if (inserted == 1) {
      return Optional.empty();
    }
    // Block until the winning transaction commits bind (or rolls back).
    List<UUID> locked =
        jdbc.query(
            """
            SELECT request_hash, operation_id
            FROM idempotency_keys
            WHERE scope = ? AND idempotency_key = ?
            FOR UPDATE
            """,
            (rs, rowNum) -> {
              String storedHash = rs.getString("request_hash");
              if (!storedHash.equals(requestHash)) {
                throw new br.com.payroll.portability.portability.application
                    .IdempotencyConflictException(
                    "Chave de idempotência reutilizada com payload diferente");
              }
              String op = rs.getString("operation_id");
              return op == null ? null : UUID.fromString(op);
            },
            scope,
            key);
    if (locked.isEmpty()) {
      throw new IllegalStateException("Idempotency key missing after conflict");
    }
    UUID operationId = locked.getFirst();
    if (operationId == null) {
      throw new IllegalStateException(
          "Idempotency claim abandoned before bind; retry the request");
    }
    return Optional.of(operationId);
  }

  @Override
  public void bindIdempotencyOperation(String scope, String key, UUID operationId) {
    jdbc.update(
        """
        UPDATE idempotency_keys
        SET operation_id = ?
        WHERE scope = ? AND idempotency_key = ? AND operation_id IS NULL
        """,
        operationId,
        scope,
        key);
  }

  @Override
  public void releaseIdempotencyClaim(String scope, String key) {
    jdbc.update(
        """
        DELETE FROM idempotency_keys
        WHERE scope = ? AND idempotency_key = ? AND operation_id IS NULL
        """,
        scope,
        key);
  }

  @Override
  @Transactional
  public void insertAtomicCreate(PortabilityOperation operation) {
    Instant now = operation.createdAt();
    jdbc.update(
        """
        INSERT INTO portability_operations (
          id, beneficiary_id, assignee_creditor, receiving_bank, receiving_agency,
          receiving_account, business_status, rejection_reason, central_protocol,
          correlation_id, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        operation.id(),
        operation.beneficiaryId(),
        operation.assigneeCreditor(),
        operation.receivingAccount().bank(),
        operation.receivingAccount().agency(),
        operation.receivingAccount().account(),
        operation.businessStatus().name(),
        operation.rejectionReason(),
        operation.centralProtocol(),
        operation.correlationId(),
        Timestamp.from(operation.createdAt()),
        Timestamp.from(operation.updatedAt()));

    for (String externalId : operation.externalInstallmentIds()) {
      UUID installmentId =
          jdbc.queryForObject(
              "SELECT id FROM installment_snapshots WHERE external_installment_id = ?",
              UUID.class,
              externalId);
      jdbc.update(
          """
          INSERT INTO operation_installments (operation_id, installment_id, external_installment_id)
          VALUES (?, ?, ?)
          """,
          operation.id(),
          installmentId,
          externalId);
      try {
        jdbc.update(
            """
            INSERT INTO installment_reservations (
              id, external_installment_id, operation_id, status, created_at
            ) VALUES (?, ?, ?, 'ACTIVE', ?)
            """,
            UUID.randomUUID(),
            externalId,
            operation.id(),
            Timestamp.from(now));
      } catch (DataIntegrityViolationException ex) {
        throw InstallmentConflictException.alreadyReserved(externalId);
      }
    }

    for (OperationEvent event : operation.pullNewEvents()) {
      jdbc.update(
          """
          INSERT INTO operation_events (
            id, operation_id, sequence_no, event_type, description,
            correlation_id, payload_redacted, occurred_at
          ) VALUES (?, ?, ?, ?, ?, ?, '{}'::jsonb, ?)
          """,
          event.id(),
          event.operationId(),
          event.sequenceNo(),
          event.eventType(),
          event.description(),
          event.correlationId(),
          Timestamp.from(event.occurredAt()));
    }

    jdbc.update(
        """
        INSERT INTO processing_jobs (
          id, operation_id, job_type, technical_status, attempt_count,
          fence_token, next_run_at, created_at, updated_at
        ) VALUES (?, ?, 'SUBMIT_TO_CENTRAL', ?, 0, 0, ?, ?, ?)
        """,
        UUID.randomUUID(),
        operation.id(),
        TechnicalJobStatus.PENDING.name(),
        Timestamp.from(now),
        Timestamp.from(now),
        Timestamp.from(now));
  }

  @Override
  public Optional<UUID> findOperationIdByIdempotency(String scope, String key) {
    List<UUID> ids =
        jdbc.query(
            """
            SELECT operation_id FROM idempotency_keys
            WHERE scope = ? AND idempotency_key = ?
            """,
            (rs, rowNum) -> UUID.fromString(rs.getString("operation_id")),
            scope,
            key);
    return ids.stream().findFirst();
  }

  @Override
  public Optional<String> findRequestHashByIdempotency(String scope, String key) {
    List<String> hashes =
        jdbc.query(
            """
            SELECT request_hash FROM idempotency_keys
            WHERE scope = ? AND idempotency_key = ?
            """,
            (rs, rowNum) -> rs.getString("request_hash"),
            scope,
            key);
    return hashes.stream().findFirst();
  }

  @Override
  public Optional<PortabilityOperation> findById(UUID operationId) {
    List<PortabilityOperation> ops =
        jdbc.query(
            "SELECT * FROM portability_operations WHERE id = ?",
            (rs, rowNum) -> {
              Set<String> installmentIds =
                  new LinkedHashSet<>(
                      jdbc.query(
                          """
                          SELECT external_installment_id FROM operation_installments
                          WHERE operation_id = ?
                          ORDER BY external_installment_id
                          """,
                          (r, n) -> r.getString("external_installment_id"),
                          operationId));
              return PortabilityOperation.reconstitute(
                  UUID.fromString(rs.getString("id")),
                  UUID.fromString(rs.getString("beneficiary_id")),
                  rs.getString("assignee_creditor"),
                  new ReceivingAccount(
                      rs.getString("receiving_bank"),
                      rs.getString("receiving_agency"),
                      rs.getString("receiving_account")),
                  installmentIds,
                  rs.getString("correlation_id"),
                  rs.getTimestamp("created_at").toInstant(),
                  BusinessStatus.valueOf(rs.getString("business_status")),
                  rs.getString("rejection_reason"),
                  rs.getString("central_protocol"),
                  rs.getTimestamp("updated_at").toInstant(),
                  nextSequence(operationId));
            },
            operationId);
    return ops.stream().findFirst();
  }

  @Override
  public void saveOutcome(PortabilityOperation operation) {
    jdbc.update(
        """
        UPDATE portability_operations
        SET business_status = ?, rejection_reason = ?, central_protocol = ?, updated_at = ?
        WHERE id = ?
        """,
        operation.businessStatus().name(),
        operation.rejectionReason(),
        operation.centralProtocol(),
        Timestamp.from(operation.updatedAt()),
        operation.id());
    insertNewEvents(operation);
  }

  @Override
  public int saveOutcomeIfPending(PortabilityOperation operation, BusinessStatus expectedStatus) {
    int updated =
        jdbc.update(
            """
            UPDATE portability_operations
            SET business_status = ?, rejection_reason = ?, central_protocol = ?, updated_at = ?
            WHERE id = ? AND business_status = ?
            """,
            operation.businessStatus().name(),
            operation.rejectionReason(),
            operation.centralProtocol(),
            Timestamp.from(operation.updatedAt()),
            operation.id(),
            expectedStatus.name());
    if (updated == 1) {
      insertNewEvents(operation);
    } else {
      operation.pullNewEvents();
    }
    return updated;
  }

  @Override
  public int appendEventsIfNonTerminal(PortabilityOperation operation) {
    int touched =
        jdbc.update(
            """
            UPDATE portability_operations
            SET updated_at = ?
            WHERE id = ? AND business_status = 'PENDENTE'
            """,
            Timestamp.from(operation.updatedAt()),
            operation.id());
    if (touched == 1) {
      insertNewEvents(operation);
    } else {
      operation.pullNewEvents();
    }
    return touched;
  }

  private void insertNewEvents(PortabilityOperation operation) {
    for (OperationEvent event : operation.pullNewEvents()) {
      jdbc.update(
          """
          INSERT INTO operation_events (
            id, operation_id, sequence_no, event_type, description,
            correlation_id, payload_redacted, occurred_at
          ) VALUES (?, ?, ?, ?, ?, ?, '{}'::jsonb, ?)
          """,
          event.id(),
          event.operationId(),
          event.sequenceNo(),
          event.eventType(),
          event.description(),
          event.correlationId(),
          Timestamp.from(event.occurredAt()));
    }
  }

  @Override
  public void consumeReservation(UUID operationId, String externalInstallmentId, Instant now) {
    jdbc.update(
        """
        UPDATE installment_reservations
        SET status = 'CONSUMED', released_at = ?
        WHERE operation_id = ? AND external_installment_id = ? AND status = 'ACTIVE'
        """,
        Timestamp.from(now),
        operationId,
        externalInstallmentId);
  }

  @Override
  public void releaseReservation(UUID operationId, String externalInstallmentId, Instant now) {
    jdbc.update(
        """
        UPDATE installment_reservations
        SET status = 'RELEASED', released_at = ?
        WHERE operation_id = ? AND external_installment_id = ? AND status = 'ACTIVE'
        """,
        Timestamp.from(now),
        operationId,
        externalInstallmentId);
  }

  @Override
  public List<OperationEvent> listEvents(UUID operationId) {
    return jdbc.query(
        """
        SELECT id, operation_id, sequence_no, event_type, description, correlation_id, occurred_at
        FROM operation_events
        WHERE operation_id = ?
        ORDER BY sequence_no
        """,
        (rs, rowNum) ->
            new OperationEvent(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("operation_id")),
                rs.getInt("sequence_no"),
                rs.getString("event_type"),
                rs.getString("description"),
                rs.getString("correlation_id"),
                rs.getTimestamp("occurred_at").toInstant()),
        operationId);
  }

  private int nextSequence(UUID operationId) {
    Integer max =
        jdbc.queryForObject(
            "SELECT COALESCE(MAX(sequence_no), 0) FROM operation_events WHERE operation_id = ?",
            Integer.class,
            operationId);
    return (max == null ? 0 : max) + 1;
  }
}
