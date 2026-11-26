package br.com.payroll.central.application;

import br.com.payroll.central.integrity.HmacCanonical;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CentralProtocolService {

  public static final String PORTABILITY_SCOPE = "central-portability";

  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Duration confirmDelay;

  public CentralProtocolService(
      JdbcTemplate jdbc,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${app.sim.confirm-delay:PT2S}") Duration confirmDelay) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.confirmDelay = confirmDelay;
  }

  @Transactional
  public Map<String, String> createAuthorization(String beneficiaryId, String agreementId) {
    String existing =
        jdbc.query(
                """
                SELECT authorization_id FROM authorizations
                WHERE beneficiary_id = ? AND agreement_id = ?
                """,
                (rs, rowNum) -> rs.getString("authorization_id"),
                beneficiaryId,
                agreementId)
            .stream()
            .findFirst()
            .orElse(null);
    if (existing != null) {
      return Map.of("autorizacaoId", existing);
    }

    String authorizationId = "auth-" + UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO authorizations (
          id, beneficiary_id, agreement_id, authorization_id, status, created_at
        ) VALUES (?, ?, ?, ?, 'ACCEPTED', ?)
        ON CONFLICT (beneficiary_id, agreement_id) DO NOTHING
        """,
        UUID.randomUUID(),
        beneficiaryId,
        agreementId,
        authorizationId,
        Timestamp.from(clock.instant()));

    String persisted =
        jdbc.queryForObject(
            """
            SELECT authorization_id FROM authorizations
            WHERE beneficiary_id = ? AND agreement_id = ?
            """,
            String.class,
            beneficiaryId,
            agreementId);
    return Map.of("autorizacaoId", persisted);
  }

  public List<Map<String, Object>> agenda(String beneficiaryId) {
    Integer authCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM authorizations
            WHERE beneficiary_id = ? AND status = 'ACCEPTED'
            """,
            Integer.class,
            beneficiaryId);
    if (authCount == null || authCount == 0) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No accepted authorization");
    }

    return jdbc.query(
        """
        SELECT external_installment_id, agreement_id, amount, discount_date, status
        FROM agenda_installments
        WHERE beneficiary_id = ? AND status = 'OPEN'
        ORDER BY discount_date
        """,
        (rs, rowNum) ->
            Map.<String, Object>of(
                "parcelaId", rs.getString("external_installment_id"),
                "convenioId", rs.getString("agreement_id"),
                "valor", rs.getBigDecimal("amount"),
                "dataDesconto", rs.getDate("discount_date").toLocalDate().toString(),
                "status", rs.getString("status")),
        beneficiaryId);
  }

  @Transactional
  public Map<String, String> createPortability(String rawBody, String idempotencyKey, String faultHeader) {
    JsonNode root;
    try {
      root = objectMapper.readTree(rawBody);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON");
    }

    String requestHash = HmacCanonical.sha256Hex(rawBody.getBytes(StandardCharsets.UTF_8));
    Optional<Map<String, String>> claimed =
        claimIdempotencyOrFind(idempotencyKey, requestHash);
    if (claimed.isPresent()) {
      return claimed.get();
    }

    FaultMode faultMode = parseFault(faultHeader, root.path("beneficiarioId").asText(""));
    if (faultMode == FaultMode.UNAVAILABLE) {
      releaseIdempotencyClaim(idempotencyKey);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated outage");
    }

    String beneficiaryId = requiredText(root, "beneficiarioId");
    String creditor = requiredText(root, "credorCessionario");
    JsonNode account = root.path("contaRecebimento");
    String bank = requiredText(account, "banco");
    String agency = requiredText(account, "agencia");
    String conta = requiredText(account, "conta");

    // Deterministic order avoids deadlocks when overlapping multi-installment ops race.
    Set<String> parcelas = new TreeSet<>();
    if (!root.path("parcelas").isArray() || root.path("parcelas").isEmpty()) {
      releaseIdempotencyClaim(idempotencyKey);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parcelas required");
    }
    root.path("parcelas").forEach(n -> parcelas.add(n.asText()));

    try {
      ensureAuthorized(beneficiaryId, parcelas);
      reserveInstallments(parcelas);

      Instant now = clock.instant();
      UUID protocolId = UUID.randomUUID();
      String protocol = "prot-" + protocolId;
      Instant confirmAfter =
          faultMode == FaultMode.TIMEOUT ? null : now.plus(confirmDelay);

      String initialStatus = "PENDENTE";
      String rejection = null;

      jdbc.update(
          """
          INSERT INTO portability_protocols (
            id, protocol, beneficiary_id, assignee_creditor, receiving_bank, receiving_agency,
            receiving_account, business_status, rejection_reason, fault_mode, confirm_after,
            correlation_id, created_at, updated_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
          protocolId,
          protocol,
          beneficiaryId,
          creditor,
          bank,
          agency,
          conta,
          initialStatus,
          rejection,
          faultMode.name(),
          confirmAfter == null ? null : Timestamp.from(confirmAfter),
          "corr-central",
          Timestamp.from(now),
          Timestamp.from(now));

      for (String parcela : parcelas) {
        jdbc.update(
            "INSERT INTO protocol_installments (protocol_id, external_installment_id) VALUES (?, ?)",
            protocolId,
            parcela);
        jdbc.update(
            """
            INSERT INTO installment_reservations (
              id, external_installment_id, protocol_id, status, created_at
            ) VALUES (?, ?, ?, 'ACTIVE', ?)
            """,
            UUID.randomUUID(),
            parcela,
            protocolId,
            Timestamp.from(now));
        jdbc.update(
            "UPDATE agenda_installments SET status = 'RESERVED' WHERE external_installment_id = ?",
            parcela);
      }

      jdbc.update(
          """
          UPDATE idempotency_keys
          SET protocol_id = ?
          WHERE scope = ? AND idempotency_key = ? AND protocol_id IS NULL
          """,
          protocolId,
          PORTABILITY_SCOPE,
          idempotencyKey);

      return Map.of("protocolo", protocol);
    } catch (RuntimeException ex) {
      releaseIdempotencyClaim(idempotencyKey);
      throw ex;
    }
  }

  @Transactional
  public ProtocolStatusView status(String protocol) {
    var rows =
        jdbc.query(
            """
            SELECT id, business_status, rejection_reason, fault_mode, confirm_after
            FROM portability_protocols
            WHERE protocol = ?
            FOR UPDATE
            """,
            (rs, rowNum) ->
                new ProtocolRow(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("business_status"),
                    rs.getString("rejection_reason"),
                    FaultMode.valueOf(rs.getString("fault_mode")),
                    rs.getTimestamp("confirm_after") == null
                        ? null
                        : rs.getTimestamp("confirm_after").toInstant()),
            protocol);
    if (rows.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol not found");
    }
    ProtocolRow row = rows.getFirst();
    Instant now = clock.instant();

    if ("PENDENTE".equals(row.status())) {
      if (row.faultMode() == FaultMode.TIMEOUT) {
        return ProtocolStatusView.pending();
      }
      if (row.confirmAfter() != null && now.isBefore(row.confirmAfter())) {
        return ProtocolStatusView.pending();
      }
      if (row.faultMode() == FaultMode.REJECT) {
        int updated =
            jdbc.update(
                """
                UPDATE portability_protocols
                SET business_status = 'REJEITADA', rejection_reason = ?, updated_at = ?
                WHERE id = ? AND business_status = 'PENDENTE'
                """,
                "Simulated rejection",
                Timestamp.from(now),
                row.id());
        if (updated != 1) {
          return reloadStatus(row.id());
        }
        releaseReservations(row.id(), false);
        return new ProtocolStatusView("REJEITADA", "Simulated rejection", false);
      }
      int updated =
          jdbc.update(
              """
              UPDATE portability_protocols
              SET business_status = 'CONFIRMADA', updated_at = ?
              WHERE id = ? AND business_status = 'PENDENTE'
              """,
              Timestamp.from(now),
              row.id());
      if (updated != 1) {
        return reloadStatus(row.id());
      }
      consumeReservations(row.id());
      return new ProtocolStatusView("CONFIRMADA", null, false);
    }

    return new ProtocolStatusView(row.status(), row.rejectionReason(), false);
  }

  private ProtocolStatusView reloadStatus(UUID protocolId) {
    var rows =
        jdbc.query(
            """
            SELECT business_status, rejection_reason
            FROM portability_protocols WHERE id = ?
            """,
            (rs, rowNum) ->
                new ProtocolStatusView(
                    rs.getString("business_status"),
                    rs.getString("rejection_reason"),
                    false),
            protocolId);
    if (rows.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocol not found");
    }
    return rows.getFirst();
  }

  private Optional<Map<String, String>> claimIdempotencyOrFind(String key, String requestHash) {
    int inserted =
        jdbc.update(
            """
            INSERT INTO idempotency_keys (
              id, scope, idempotency_key, request_hash, protocol_id, created_at
            ) VALUES (?, ?, ?, ?, NULL, ?)
            ON CONFLICT (scope, idempotency_key) DO NOTHING
            """,
            UUID.randomUUID(),
            PORTABILITY_SCOPE,
            key,
            requestHash,
            Timestamp.from(clock.instant()));
    if (inserted == 1) {
      return Optional.empty();
    }
    var rows =
        jdbc.query(
            """
            SELECT k.request_hash, p.protocol
            FROM idempotency_keys k
            LEFT JOIN portability_protocols p ON p.id = k.protocol_id
            WHERE k.scope = ? AND k.idempotency_key = ?
            FOR UPDATE OF k
            """,
            (rs, rowNum) ->
                Map.entry(
                    rs.getString("request_hash"),
                    rs.getString("protocol")),
            PORTABILITY_SCOPE,
            key);
    if (rows.isEmpty()) {
      throw new IllegalStateException("Idempotency key missing after conflict");
    }
    var entry = rows.getFirst();
    if (!entry.getKey().equals(requestHash)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key payload mismatch");
    }
    if (entry.getValue() == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Idempotency claim in flight; retry");
    }
    return Optional.of(Map.of("protocolo", entry.getValue()));
  }

  private void releaseIdempotencyClaim(String key) {
    jdbc.update(
        """
        DELETE FROM idempotency_keys
        WHERE scope = ? AND idempotency_key = ? AND protocol_id IS NULL
        """,
        PORTABILITY_SCOPE,
        key);
  }

  private void ensureAuthorized(String beneficiaryId, Set<String> parcelas) {
    for (String parcela : parcelas) {
      var rows =
          jdbc.query(
              """
              SELECT agreement_id, status FROM agenda_installments
              WHERE external_installment_id = ? AND beneficiary_id = ?
              """,
              (rs, rowNum) -> Map.entry(rs.getString("agreement_id"), rs.getString("status")),
              parcela,
              beneficiaryId);
      if (rows.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown installment " + parcela);
      }
      if (!"OPEN".equals(rows.getFirst().getValue())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Installment not open: " + parcela);
      }
      String agreementId = rows.getFirst().getKey();
      Integer auth =
          jdbc.queryForObject(
              """
              SELECT COUNT(*) FROM authorizations
              WHERE beneficiary_id = ? AND agreement_id = ? AND status = 'ACCEPTED'
              """,
              Integer.class,
              beneficiaryId,
              agreementId);
      if (auth == null || auth == 0) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized agreement");
      }
    }
  }

  private void reserveInstallments(Set<String> parcelas) {
    // uniqueness enforced by DB index during insert
  }

  private void releaseReservations(UUID protocolId, boolean ported) {
    jdbc.update(
        """
        UPDATE installment_reservations SET status = 'RELEASED'
        WHERE protocol_id = ? AND status = 'ACTIVE'
        """,
        protocolId);
    // Rejection keeps vigente (effective_*) unchanged.
    jdbc.update(
        """
        UPDATE agenda_installments a
        SET status = 'OPEN'
        FROM protocol_installments pi
        WHERE pi.protocol_id = ? AND pi.external_installment_id = a.external_installment_id
        """,
        protocolId);
  }

  private void consumeReservations(UUID protocolId) {
    jdbc.update(
        """
        UPDATE installment_reservations SET status = 'CONSUMED'
        WHERE protocol_id = ? AND status = 'ACTIVE'
        """,
        protocolId);
    jdbc.update(
        """
        UPDATE agenda_installments a
        SET status = 'PORTED',
            effective_bank = p.receiving_bank,
            effective_agency = p.receiving_agency,
            effective_account = p.receiving_account
        FROM protocol_installments pi
        JOIN portability_protocols p ON p.id = pi.protocol_id
        WHERE pi.protocol_id = ? AND pi.external_installment_id = a.external_installment_id
        """,
        protocolId);
  }

  private FaultMode parseFault(String header, String beneficiaryId) {
    if (header != null && !header.isBlank()) {
      return FaultMode.valueOf(header.trim().toUpperCase());
    }
    if (beneficiaryId.endsWith("-timeout")) {
      return FaultMode.TIMEOUT;
    }
    if (beneficiaryId.endsWith("-reject")) {
      return FaultMode.REJECT;
    }
    if (beneficiaryId.endsWith("-503")) {
      return FaultMode.UNAVAILABLE;
    }
    return FaultMode.CONFIRM;
  }

  private static String requiredText(JsonNode node, String field) {
    String value = node.path(field).asText(null);
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " required");
    }
    return value;
  }

  public record ProtocolStatusView(String status, String motivoRejeicao, boolean pendingEmpty) {
    static ProtocolStatusView pending() {
      return new ProtocolStatusView(null, null, true);
    }
  }

  private record ProtocolRow(
      UUID id, String status, String rejectionReason, FaultMode faultMode, Instant confirmAfter) {}
}
