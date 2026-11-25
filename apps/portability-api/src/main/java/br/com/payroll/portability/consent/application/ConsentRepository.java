package br.com.payroll.portability.consent.application;

import br.com.payroll.portability.consent.domain.Consent;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository {

  void save(Consent consent);

  Optional<Consent> findAccepted(UUID beneficiaryId, String agreementId);

  boolean hasAcceptedConsent(UUID beneficiaryId);
}
