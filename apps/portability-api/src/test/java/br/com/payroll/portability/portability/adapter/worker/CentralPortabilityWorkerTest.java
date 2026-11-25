package br.com.payroll.portability.portability.adapter.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.payroll.portability.consent.application.ConsentRepository;
import br.com.payroll.portability.consent.domain.Consent;
import br.com.payroll.portability.integration.central.CentralException;
import br.com.payroll.portability.integration.central.CentralGateway;
import br.com.payroll.portability.integration.central.CentralPortabilityRequest;
import br.com.payroll.portability.integration.central.CentralStatus;
import br.com.payroll.portability.portability.application.CreatePortabilityService;
import br.com.payroll.portability.portability.application.PortabilityOperationRepository;
import br.com.payroll.portability.portability.application.ProcessingJob;
import br.com.payroll.portability.portability.application.ProcessingJobRepository;
import br.com.payroll.portability.portability.domain.BusinessStatus;
import br.com.payroll.portability.portability.domain.PortabilityCommand;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import br.com.payroll.portability.portability.domain.TechnicalJobStatus;
import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import br.com.payroll.portability.schedule.domain.InstallmentStatus;
import br.com.payroll.portability.shared.domain.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CentralPortabilityWorkerTest {

  @Container
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("portability")
          .withUsername("portability")
          .withPassword("portability");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("app.central.worker-enabled", () -> "false");
    registry.add("app.central.max-attempts", () -> "3");
    registry.add("app.central.poll-delay", () -> "PT0.01S");
  }

  @TestConfiguration
  static class FakeCentralConfig {
    @Bean
    @Primary
    CentralGateway fakeCentralGateway() {
      return FakeCentral.INSTANCE;
    }
  }

  @Autowired CreatePortabilityService createService;
  @Autowired ConsentRepository consentRepository;
  @Autowired InstallmentRepository installmentRepository;
  @Autowired PortabilityOperationRepository operationRepository;
  @Autowired ProcessingJobRepository jobRepository;
  @Autowired CentralPortabilityWorker worker;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;

  private UUID beneficiaryId;

  @BeforeEach
  void setUp() {
    FakeCentral.INSTANCE.reset();
    jdbc.update("DELETE FROM idempotency_keys");
    jdbc.update("DELETE FROM processing_jobs");
    jdbc.update("DELETE FROM operation_events");
    jdbc.update("DELETE FROM installment_reservations");
    jdbc.update("DELETE FROM operation_installments");
    jdbc.update("DELETE FROM portability_operations");
    jdbc.update("DELETE FROM installment_snapshots");
    jdbc.update("DELETE FROM consents");
    jdbc.update("DELETE FROM beneficiaries");

    beneficiaryId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO beneficiaries (id, external_ref, display_name) VALUES (?, ?, ?)",
        beneficiaryId,
        "ben-001",
        "Synthetic Beneficiary");

    Instant now = Instant.now(clock);
    Consent consent = Consent.request(beneficiaryId, "conv-1", now);
    consent.accept("auth-1", now);
    consentRepository.save(consent);

    installmentRepository.save(
        new InstallmentSnapshot(
            UUID.randomUUID(),
            "parcela-w1",
            beneficiaryId,
            "conv-1",
            Money.of("100.00"),
            LocalDate.of(2026, 12, 10),
            InstallmentStatus.OPEN));
  }

  @Test
  void confirmPathConsumesReservation() {
    FakeCentral.INSTANCE.mode = FakeCentral.Mode.CONFIRM;
    UUID operationId = createOp("idem-confirm");

    ProcessingJob submit =
        jobRepository.claimNext("test", clock.instant(), clock.instant().plusSeconds(30)).orElseThrow();
    worker.processClaimed(submit);

    Instant later = clock.instant().plusSeconds(5);
    ProcessingJob poll =
        jobRepository.claimNext("test", later, later.plusSeconds(30)).orElseThrow();
    worker.processClaimed(poll);

    assertEquals(
        BusinessStatus.CONFIRMADA, operationRepository.findById(operationId).orElseThrow().businessStatus());
    assertEquals(
        InstallmentStatus.PORTED,
        installmentRepository.findByExternalId("parcela-w1").orElseThrow().status());
    Integer consumed =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM installment_reservations WHERE status = 'CONSUMED'", Integer.class);
    assertEquals(1, consumed);
  }

  @Test
  void rejectPathReleasesReservation() {
    FakeCentral.INSTANCE.mode = FakeCentral.Mode.REJECT;
    UUID operationId = createOp("idem-reject");

    ProcessingJob submit =
        jobRepository.claimNext("test", clock.instant(), clock.instant().plusSeconds(30)).orElseThrow();
    worker.processClaimed(submit);
    Instant later = clock.instant().plusSeconds(5);
    ProcessingJob poll =
        jobRepository.claimNext("test", later, later.plusSeconds(30)).orElseThrow();
    worker.processClaimed(poll);

    assertEquals(
        BusinessStatus.REJEITADA, operationRepository.findById(operationId).orElseThrow().businessStatus());
    assertEquals(
        InstallmentStatus.OPEN,
        installmentRepository.findByExternalId("parcela-w1").orElseThrow().status());
    Integer released =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM installment_reservations WHERE status = 'RELEASED'", Integer.class);
    assertEquals(1, released);
  }

  @Test
  void transientFailureDoesNotRejectBusiness() {
    FakeCentral.INSTANCE.mode = FakeCentral.Mode.UNAVAILABLE;
    UUID operationId = createOp("idem-503");

    ProcessingJob submit =
        jobRepository.claimNext("test", clock.instant(), clock.instant().plusSeconds(30)).orElseThrow();
    worker.processClaimed(submit);

    assertEquals(
        BusinessStatus.PENDENTE, operationRepository.findById(operationId).orElseThrow().businessStatus());
    assertNull(operationRepository.findById(operationId).orElseThrow().centralProtocol());
    String jobStatus =
        jdbc.queryForObject(
            "SELECT technical_status FROM processing_jobs WHERE operation_id = ?",
            String.class,
            operationId);
    assertTrue(
        TechnicalJobStatus.PENDING.name().equals(jobStatus)
            || TechnicalJobStatus.EXHAUSTED.name().equals(jobStatus));
  }

  private UUID createOp(String idem) {
    return createService.create(
        new PortabilityCommand(
            beneficiaryId,
            Set.of("parcela-w1"),
            "creditor-x",
            new ReceivingAccount("001", "1234", "56789-0"),
            idem,
            "corr-" + idem,
            "hash-" + idem));
  }

  static final class FakeCentral implements CentralGateway {
    static final FakeCentral INSTANCE = new FakeCentral();

    enum Mode {
      CONFIRM,
      REJECT,
      UNAVAILABLE
    }

    Mode mode = Mode.CONFIRM;
    final AtomicInteger submits = new AtomicInteger();
    final AtomicReference<String> protocol = new AtomicReference<>();

    void reset() {
      mode = Mode.CONFIRM;
      submits.set(0);
      protocol.set(null);
    }

    @Override
    public String createAuthorization(String beneficiaryExternalRef, String agreementId, String correlationId) {
      return "auth-fake";
    }

    @Override
    public List<br.com.payroll.portability.integration.central.CentralAgendaItem> fetchAgenda(
        String beneficiaryExternalRef, String correlationId) {
      return List.of();
    }

    @Override
    public String submitPortability(
        CentralPortabilityRequest request, String idempotencyKey, String correlationId) {
      if (mode == Mode.UNAVAILABLE) {
        throw new CentralException("simulated 503", true, 503);
      }
      submits.incrementAndGet();
      String p = "prot-" + UUID.randomUUID();
      protocol.set(p);
      return p;
    }

    @Override
    public CentralStatus pollStatus(String protocol, String correlationId) {
      if (mode == Mode.CONFIRM) {
        return CentralStatus.terminal("CONFIRMADA", null);
      }
      if (mode == Mode.REJECT) {
        return CentralStatus.terminal("REJEITADA", "Simulated rejection");
      }
      throw new CentralException("simulated 503", true, 503);
    }
  }
}
