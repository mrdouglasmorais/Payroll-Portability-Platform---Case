package br.com.payroll.portability.consent.adapter.persistence;

import br.com.payroll.portability.consent.application.ConsentRepository;
import br.com.payroll.portability.consent.domain.Consent;
import br.com.payroll.portability.consent.domain.ConsentStatus;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcConsentRepository implements ConsentRepository {

  private static final RowMapper<Consent> MAPPER =
      (rs, rowNum) ->
          Consent.reconstitute(
              UUID.fromString(rs.getString("id")),
              UUID.fromString(rs.getString("beneficiary_id")),
              rs.getString("agreement_id"),
              rs.getString("authorization_id"),
              ConsentStatus.valueOf(rs.getString("status")),
              rs.getTimestamp("created_at").toInstant(),
              rs.getTimestamp("updated_at").toInstant());

  private final JdbcTemplate jdbc;

  public JdbcConsentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void save(Consent consent) {
    Integer exists =
        jdbc.queryForObject("SELECT COUNT(*) FROM consents WHERE id = ?", Integer.class, consent.id());
    if (exists != null && exists > 0) {
      jdbc.update(
          """
          UPDATE consents
          SET authorization_id = ?, status = ?, updated_at = ?
          WHERE id = ?
          """,
          consent.authorizationId(),
          consent.status().name(),
          Timestamp.from(consent.updatedAt()),
          consent.id());
      return;
    }
    jdbc.update(
        """
        INSERT INTO consents (
          id, beneficiary_id, agreement_id, authorization_id, status, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        consent.id(),
        consent.beneficiaryId(),
        consent.agreementId(),
        consent.authorizationId(),
        consent.status().name(),
        Timestamp.from(consent.createdAt()),
        Timestamp.from(consent.updatedAt()));
  }

  @Override
  public Optional<Consent> findAccepted(UUID beneficiaryId, String agreementId) {
    var list =
        jdbc.query(
            """
            SELECT * FROM consents
            WHERE beneficiary_id = ? AND agreement_id = ? AND status = 'ACCEPTED'
            """,
            MAPPER,
            beneficiaryId,
            agreementId);
    return list.stream().findFirst();
  }

  @Override
  public boolean hasAcceptedConsent(UUID beneficiaryId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM consents WHERE beneficiary_id = ? AND status = 'ACCEPTED'",
            Integer.class,
            beneficiaryId);
    return count != null && count > 0;
  }
}
