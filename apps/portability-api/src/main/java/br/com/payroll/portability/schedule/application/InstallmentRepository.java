package br.com.payroll.portability.schedule.application;

import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstallmentRepository {

  void save(InstallmentSnapshot installment);

  /**
   * Atomically marks an OPEN installment as RESERVED. Returns {@code 1} on win, {@code 0} if
   * another transaction already reserved or ported it (safe under concurrent create).
   */
  int reserveIfOpen(UUID installmentId);

  /**
   * Updates only mutable agenda attributes for rows that are still OPEN. Never touches
   * status or effective receiving account (avoids racing with portability finalization).
   *
   * @return rows updated (0 if the installment left OPEN concurrently)
   */
  int updateOpenAgendaAttributes(InstallmentSnapshot installment);

  Optional<InstallmentSnapshot> findByExternalId(String externalInstallmentId);

  List<InstallmentSnapshot> findByExternalIds(Collection<String> externalIds);

  List<InstallmentSnapshot> findOpenByBeneficiaryAndAgreement(UUID beneficiaryId, String agreementId);
}
