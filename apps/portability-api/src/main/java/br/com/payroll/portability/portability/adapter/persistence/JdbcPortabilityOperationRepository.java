package br.com.payroll.portability.portability.adapter.persistence;

import br.com.payroll.portability.portability.application.PortabilityOperationRepository;
import br.com.payroll.portability.portability.domain.BusinessStatus;
import br.com.payroll.portability.portability.domain.OperationEvent;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import br.com.payroll.portability.portability.domain.TechnicalJobStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
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
  @Transactional
  public void insertAtomicCreate(
      PortabilityOperation operation,
      String idempotencyScope,
      String idempotencyKey,
      String requestHash) {
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
      } catch (DuplicateKeyException ex) {
        throw new IllegalStateException(
            "Installment already reserved: " + externalId, ex);
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

    jdbc.update(
        """
        INSERT INTO idempotency_keys (
          id, scope, idempotency_key, request_hash, operation_id, created_at
        ) VALUES (?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        idempotencyScope,
        idempotencyKey,
        requestHash,
        operation.id(),
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

  private int nextSequence(UUID operationId) {
    Integer max =
        jdbc.queryForObject(
            "SELECT COALESCE(MAX(sequence_no), 0) FROM operation_events WHERE operation_id = ?",
            Integer.class,
            operationId);
    return (max == null ? 0 : max) + 1;
  }
}
