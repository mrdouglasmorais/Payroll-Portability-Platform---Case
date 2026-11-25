package br.com.payroll.portability.beneficiary.application;

import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryRepository {

  UUID findOrCreate(String externalRef, String displayName);

  Optional<UUID> findIdByExternalRef(String externalRef);

  Optional<String> findExternalRefById(UUID beneficiaryId);
}
