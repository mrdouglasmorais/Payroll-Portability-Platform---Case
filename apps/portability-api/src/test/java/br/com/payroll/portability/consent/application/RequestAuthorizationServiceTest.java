package br.com.payroll.portability.consent.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.payroll.portability.beneficiary.application.BeneficiaryRepository;
import br.com.payroll.portability.consent.domain.Consent;
import br.com.payroll.portability.consent.domain.ConsentStatus;
import br.com.payroll.portability.integration.central.CentralAgendaItem;
import br.com.payroll.portability.integration.central.CentralException;
import br.com.payroll.portability.integration.central.CentralGateway;
import br.com.payroll.portability.integration.central.CentralPortabilityRequest;
import br.com.payroll.portability.integration.central.CentralStatus;
import br.com.payroll.portability.shared.domain.DomainException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class RequestAuthorizationServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");
  private static final UUID BENEFICIARY_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

  private RecordingCentral central;
  private InMemoryConsentRepository consents;
  private RequestAuthorizationService service;

  @BeforeEach
  void setUp() {
    central = new RecordingCentral();
    consents = new InMemoryConsentRepository();
    BeneficiaryRepository beneficiaries =
        new BeneficiaryRepository() {
          @Override
          public UUID findOrCreate(String externalRef, String displayName) {
            return BENEFICIARY_ID;
          }

          @Override
          public Optional<UUID> findIdByExternalRef(String externalRef) {
            return Optional.of(BENEFICIARY_ID);
          }

          @Override
          public Optional<String> findExternalRefById(UUID beneficiaryId) {
            return Optional.of("ben-001");
          }
        };
    TransactionTemplate tx =
        new TransactionTemplate() {
          @Override
          public <T> T execute(TransactionCallback<T> action) throws TransactionException {
            return action.doInTransaction(null);
          }
        };
    service =
        new RequestAuthorizationService(
            beneficiaries, consents, central, Clock.fixed(NOW, ZoneOffset.UTC), tx);
  }

  @Test
  void existingConsentStillCallsCentralToHealDesync() {
    Consent consent = Consent.request(BENEFICIARY_ID, "conv-1", NOW);
    consent.accept("auth-local", NOW);
    consents.save(consent);

    String returned = service.authorize("ben-001", "conv-1", "corr-1");

    assertEquals("auth-local", returned);
    assertEquals(1, central.createCalls.get());
  }

  @Test
  void newConsentPersistsCentralAuthorizationId() {
    central.nextAuthId = "auth-from-central";

    String returned = service.authorize("ben-new", "conv-1", "corr-2");

    assertEquals("auth-from-central", returned);
    assertEquals(1, central.createCalls.get());
    assertEquals(
        "auth-from-central",
        consents.findAccepted(BENEFICIARY_ID, "conv-1").orElseThrow().authorizationId());
  }

  @Test
  void permanentCentralFailureSurfacesAsDomainException() {
    central.failPermanent = true;

    assertThrows(
        DomainException.class, () -> service.authorize("ben-001", "conv-1", "corr-3"));
  }

  private static final class RecordingCentral implements CentralGateway {
    final AtomicInteger createCalls = new AtomicInteger();
    String nextAuthId = "auth-central";
    boolean failPermanent;

    @Override
    public String createAuthorization(
        String beneficiaryExternalRef, String agreementId, String correlationId) {
      createCalls.incrementAndGet();
      if (failPermanent) {
        throw new CentralException("forbidden", false, 403);
      }
      return nextAuthId;
    }

    @Override
    public List<CentralAgendaItem> fetchAgenda(
        String beneficiaryExternalRef, String correlationId) {
      return List.of();
    }

    @Override
    public String submitPortability(
        CentralPortabilityRequest request, String idempotencyKey, String correlationId) {
      return "prot";
    }

    @Override
    public CentralStatus pollStatus(String protocol, String correlationId) {
      return CentralStatus.pending();
    }
  }

  private static final class InMemoryConsentRepository implements ConsentRepository {
    private Consent stored;

    @Override
    public void save(Consent consent) {
      stored = consent;
    }

    @Override
    public Optional<Consent> findAccepted(UUID beneficiaryId, String agreementId) {
      if (stored == null) {
        return Optional.empty();
      }
      if (stored.beneficiaryId().equals(beneficiaryId)
          && stored.agreementId().equals(agreementId)
          && stored.status() == ConsentStatus.ACCEPTED) {
        return Optional.of(stored);
      }
      return Optional.empty();
    }

    @Override
    public boolean hasAcceptedConsent(UUID beneficiaryId) {
      return stored != null
          && stored.beneficiaryId().equals(beneficiaryId)
          && stored.status() == ConsentStatus.ACCEPTED;
    }
  }
}
