package br.com.payroll.portability.beneficiary.adapter.persistence;

import br.com.payroll.portability.beneficiary.application.BeneficiaryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBeneficiaryRepository implements BeneficiaryRepository {

  private final JdbcTemplate jdbc;

  public JdbcBeneficiaryRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public UUID findOrCreate(String externalRef, String displayName) {
    Optional<UUID> existing = findIdByExternalRef(externalRef);
    if (existing.isPresent()) {
      return existing.get();
    }
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO beneficiaries (id, external_ref, display_name)
        VALUES (?, ?, ?)
        ON CONFLICT (external_ref) DO NOTHING
        """,
        id,
        externalRef,
        displayName);
    return findIdByExternalRef(externalRef).orElseThrow();
  }

  @Override
  public Optional<UUID> findIdByExternalRef(String externalRef) {
    var list =
        jdbc.query(
            "SELECT id FROM beneficiaries WHERE external_ref = ?",
            (rs, rowNum) -> UUID.fromString(rs.getString("id")),
            externalRef);
    return list.stream().findFirst();
  }

  @Override
  public Optional<String> findExternalRefById(UUID beneficiaryId) {
    var list =
        jdbc.query(
            "SELECT external_ref FROM beneficiaries WHERE id = ?",
            (rs, rowNum) -> rs.getString("external_ref"),
            beneficiaryId);
    return list.stream().findFirst();
  }
}
