package br.com.payroll.portability.schedule.adapter.persistence;

import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import br.com.payroll.portability.schedule.domain.InstallmentStatus;
import br.com.payroll.portability.shared.domain.Money;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInstallmentRepository implements InstallmentRepository {

  private static final RowMapper<InstallmentSnapshot> MAPPER =
      (rs, rowNum) ->
          new InstallmentSnapshot(
              UUID.fromString(rs.getString("id")),
              rs.getString("external_installment_id"),
              UUID.fromString(rs.getString("beneficiary_id")),
              rs.getString("agreement_id"),
              Money.of(rs.getBigDecimal("amount")),
              rs.getDate("discount_date").toLocalDate(),
              InstallmentStatus.valueOf(rs.getString("status")));

  private final JdbcTemplate jdbc;

  public JdbcInstallmentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void save(InstallmentSnapshot installment) {
    Integer exists =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM installment_snapshots WHERE id = ?",
            Integer.class,
            installment.id());
    if (exists != null && exists > 0) {
      jdbc.update(
          "UPDATE installment_snapshots SET status = ? WHERE id = ?",
          installment.status().name(),
          installment.id());
      return;
    }
    jdbc.update(
        """
        INSERT INTO installment_snapshots (
          id, external_installment_id, beneficiary_id, agreement_id,
          amount, discount_date, status, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        installment.id(),
        installment.externalInstallmentId(),
        installment.beneficiaryId(),
        installment.agreementId(),
        installment.amount().amount(),
        Date.valueOf(installment.discountDate()),
        installment.status().name(),
        Timestamp.from(Instant.now()));
  }

  @Override
  public Optional<InstallmentSnapshot> findByExternalId(String externalInstallmentId) {
    var list =
        jdbc.query(
            "SELECT * FROM installment_snapshots WHERE external_installment_id = ?",
            MAPPER,
            externalInstallmentId);
    return list.stream().findFirst();
  }

  @Override
  public List<InstallmentSnapshot> findByExternalIds(Collection<String> externalIds) {
    if (externalIds == null || externalIds.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", Collections.nCopies(externalIds.size(), "?"));
    return jdbc.query(
        "SELECT * FROM installment_snapshots WHERE external_installment_id IN (" + placeholders + ")",
        MAPPER,
        externalIds.toArray());
  }

  @Override
  public List<InstallmentSnapshot> findOpenByBeneficiaryAndAgreement(
      UUID beneficiaryId, String agreementId) {
    return jdbc.query(
        """
        SELECT * FROM installment_snapshots
        WHERE beneficiary_id = ? AND agreement_id = ? AND status = 'OPEN'
        ORDER BY discount_date
        """,
        MAPPER,
        beneficiaryId,
        agreementId);
  }
}
