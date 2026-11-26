package br.com.payroll.portability.consent.application;

import br.com.payroll.portability.beneficiary.application.BeneficiaryRepository;
import br.com.payroll.portability.consent.domain.Consent;
import br.com.payroll.portability.integration.central.CentralException;
import br.com.payroll.portability.integration.central.CentralGateway;
import br.com.payroll.portability.shared.domain.DomainException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RequestAuthorizationService {

  private final BeneficiaryRepository beneficiaryRepository;
  private final ConsentRepository consentRepository;
  private final CentralGateway centralGateway;
  private final Clock clock;
  private final TransactionTemplate tx;

  public RequestAuthorizationService(
      BeneficiaryRepository beneficiaryRepository,
      ConsentRepository consentRepository,
      CentralGateway centralGateway,
      Clock clock,
      TransactionTemplate tx) {
    this.beneficiaryRepository = beneficiaryRepository;
    this.consentRepository = consentRepository;
    this.centralGateway = centralGateway;
    this.clock = clock;
    this.tx = tx;
  }

  public String authorize(String beneficiaryExternalRef, String agreementId, String correlationId) {
    if (beneficiaryExternalRef == null || beneficiaryExternalRef.isBlank()) {
      throw new DomainException("beneficiarioId é obrigatório");
    }
    if (agreementId == null || agreementId.isBlank()) {
      throw new DomainException("convenioId é obrigatório");
    }

    UUID beneficiaryId =
        beneficiaryRepository.findOrCreate(beneficiaryExternalRef, beneficiaryExternalRef);

    var existing = consentRepository.findAccepted(beneficiaryId, agreementId);
    if (existing.isPresent()) {
      return existing.get().authorizationId();
    }

    String authorizationId;
    try {
      authorizationId =
          centralGateway.createAuthorization(beneficiaryExternalRef, agreementId, correlationId);
    } catch (CentralException ex) {
      if (ex.isTransientFailure()) {
        throw ex;
      }
      throw new DomainException("Central rejeitou a autorização: " + ex.getMessage());
    }

    tx.executeWithoutResult(
        status -> {
          if (consentRepository.findAccepted(beneficiaryId, agreementId).isPresent()) {
            return;
          }
          Instant now = clock.instant();
          Consent consent = Consent.request(beneficiaryId, agreementId, now);
          consent.accept(authorizationId, now);
          consentRepository.save(consent);
        });
    return authorizationId;
  }
}
