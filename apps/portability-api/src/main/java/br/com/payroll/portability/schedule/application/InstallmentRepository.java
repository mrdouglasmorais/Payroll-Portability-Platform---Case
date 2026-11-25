package br.com.payroll.portability.schedule.application;

import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstallmentRepository {

  void save(InstallmentSnapshot installment);

  Optional<InstallmentSnapshot> findByExternalId(String externalInstallmentId);

  List<InstallmentSnapshot> findByExternalIds(Collection<String> externalIds);

  List<InstallmentSnapshot> findOpenByBeneficiaryAndAgreement(UUID beneficiaryId, String agreementId);
}
