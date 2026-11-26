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

    var before = installmentRepository.findByExternalId("parcela-w1").orElseThrow();
    assertEquals("000", before.effectiveAccount().bank());

    ProcessingJob submit =
        jobRepository.claimNext("test", clock.instant(), clock.instant().plusSeconds(30)).orElseThrow();
    worker.processClaimed(submit);

    Instant later = clock.instant().plusSeconds(5);
    ProcessingJob poll =
        jobRepository.claimNext("test", later, later.plusSeconds(30)).orElseThrow();
    worker.processClaimed(poll);

    assertEquals(
        BusinessStatus.CONFIRMADA, operationRepository.findById(operationId).orElseThrow().businessStatus());
    var after = installmentRepository.findByExternalId("parcela-w1").orElseThrow();
    assertEquals(InstallmentStatus.PORTED, after.status());
    assertEquals("001", after.effectiveAccount().bank());
    assertEquals("1234", after.effectiveAccount().agency());
    assertEquals("56789-0", after.effectiveAccount().account());
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
    var after = installmentRepository.findByExternalId("parcela-w1").orElseThrow();
    assertEquals(InstallmentStatus.OPEN, after.status());
    assertEquals("000", after.effectiveAccount().bank());
    assertEquals("0001", after.effectiveAccount().agency());
    assertEquals("00000-0", after.effectiveAccount().account());
    Integer released =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM installment_reservations WHERE status = 'RELEASED'", Integer.class);
    assertEquals(1, released);
  }

  @Test
  void expiredRunningLeaseIsReclaimedByAnotherWorker() {
    FakeCentral.INSTANCE.mode = FakeCentral.Mode.CONFIRM;
    UUID operationId = createOp("idem-running-lease");

    ProcessingJob first =
        jobRepository
            .claimNext("worker-a", clock.instant(), clock.instant().plusSeconds(30))
            .orElseThrow();
    assertEquals(TechnicalJobStatus.RUNNING, first.technicalStatus());

    // Simulate crash while RUNNING: lease expires, status stays RUNNING.
    jdbc.update(
        """
        UPDATE processing_jobs
        SET lease_until = ?, lease_owner = 'worker-a'
        WHERE id = ?
        """,
        java.sql.Timestamp.from(clock.instant().minusSeconds(5)),
        first.id());

    Instant later = clock.instant().plusSeconds(1);
    ProcessingJob recovered =
        jobRepository.claimNext("worker-b", later, later.plusSeconds(30)).orElseThrow();
    assertEquals(first.id(), recovered.id());
    assertEquals("worker-b", recovered.leaseOwner());
    assertTrue(recovered.fenceToken() > first.fenceToken());

    worker.processClaimed(recovered);
    Instant pollAt = clock.instant().plusSeconds(5);
    ProcessingJob poll =
        jobRepository.claimNext("worker-b", pollAt, pollAt.plusSeconds(30)).orElseThrow();
    worker.processClaimed(poll);

    assertEquals(
        BusinessStatus.CONFIRMADA,
        operationRepository.findById(operationId).orElseThrow().businessStatus());
  }

  @Test
  void staleFenceDoesNotMutateFinancialState() {
    FakeCentral.INSTANCE.mode = FakeCentral.Mode.CONFIRM;
    UUID operationId = createOp("idem-stale-fence");

    ProcessingJob submit =
        jobRepository.claimNext("test", clock.instant(), clock.instant().plusSeconds(30)).orElseThrow();
    worker.processClaimed(submit);

    Instant later = clock.instant().plusSeconds(5);
    ProcessingJob poll =
        jobRepository.claimNext("owner", later, later.plusSeconds(30)).orElseThrow();

    // Another worker reclaims with a higher fence before the stale worker finishes.
    jdbc.update(
        """
        UPDATE processing_jobs
        SET lease_until = ?, lease_owner = 'owner'
        WHERE id = ?
        """,
        java.sql.Timestamp.from(later.minusSeconds(10)),
        poll.id());
    ProcessingJob fresh =
        jobRepository
            .claimNext("fresh", later.plusSeconds(1), later.plusSeconds(31))
            .orElseThrow();
    assertTrue(fresh.fenceToken() > poll.fenceToken());

    worker.processClaimed(poll); // stale fence — must not confirm
    assertEquals(
        BusinessStatus.PENDENTE,
        operationRepository.findById(operationId).orElseThrow().businessStatus());
    assertEquals(
        "000",
        installmentRepository.findByExternalId("parcela-w1").orElseThrow().effectiveAccount().bank());

    worker.processClaimed(fresh);
    assertEquals(
        BusinessStatus.CONFIRMADA,
        operationRepository.findById(operationId).orElseThrow().businessStatus());
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

  @Test
  void lostAcceptanceResponseRecoversSameProtocolOnRetry() {
    FakeCentral.INSTANCE.mode = FakeCentral.Mode.LOST_FIRST_RESPONSE;
    FakeCentral.INSTANCE.dropRemaining.set(1);
    UUID operationId = createOp("idem-lost");

    ProcessingJob first =
        jobRepository.claimNext("test", clock.instant(), clock.instant().plusSeconds(30)).orElseThrow();
    worker.processClaimed(first);

    assertNull(operationRepository.findById(operationId).orElseThrow().centralProtocol());
    assertEquals(BusinessStatus.PENDENTE, operationRepository.findById(operationId).orElseThrow().businessStatus());
    String acceptedProtocol = FakeCentral.INSTANCE.protocol.get();
    assertTrue(acceptedProtocol != null && !acceptedProtocol.isBlank());

    Instant later = clock.instant().plusSeconds(5);
    ProcessingJob retry =
        jobRepository.claimNext("test", later, later.plusSeconds(30)).orElseThrow();
    worker.processClaimed(retry);

    assertEquals(
        acceptedProtocol,
        operationRepository.findById(operationId).orElseThrow().centralProtocol());
    assertEquals(2, FakeCentral.INSTANCE.submits.get());

    Instant pollAt = clock.instant().plusSeconds(10);
    ProcessingJob poll =
        jobRepository.claimNext("test", pollAt, pollAt.plusSeconds(30)).orElseThrow();
    worker.processClaimed(poll);

    assertEquals(
        BusinessStatus.CONFIRMADA, operationRepository.findById(operationId).orElseThrow().businessStatus());
  }

  @Test
  void restartResumesPollWithoutResubmitting() {
    FakeCentral.INSTANCE.mode = FakeCentral.Mode.CONFIRM;
    UUID operationId = createOp("idem-restart");

    ProcessingJob submit =
        jobRepository.claimNext("test", clock.instant(), clock.instant().plusSeconds(30)).orElseThrow();
    worker.processClaimed(submit);
    String protocol = operationRepository.findById(operationId).orElseThrow().centralProtocol();
    assertTrue(protocol != null && !protocol.isBlank());
    assertEquals(1, FakeCentral.INSTANCE.submits.get());

    // Simulate process crash: lease expired, poll job still pending.
    jdbc.update(
        """
        UPDATE processing_jobs
        SET technical_status = 'PENDING',
            lease_owner = NULL,
            lease_until = NULL,
            next_run_at = ?
        WHERE operation_id = ?
        """,
        java.sql.Timestamp.from(clock.instant().minusSeconds(1)),
        operationId);

    Instant later = clock.instant().plusSeconds(5);
    ProcessingJob poll =
        jobRepository.claimNext("restart-worker", later, later.plusSeconds(30)).orElseThrow();
    assertEquals(CentralPortabilityWorker.POLL, poll.jobType());
    worker.processClaimed(poll);

    assertEquals(
        BusinessStatus.CONFIRMADA, operationRepository.findById(operationId).orElseThrow().businessStatus());
    assertEquals(1, FakeCentral.INSTANCE.submits.get());
    assertEquals(
        protocol, operationRepository.findById(operationId).orElseThrow().centralProtocol());
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
      UNAVAILABLE,
      LOST_FIRST_RESPONSE
    }

    Mode mode = Mode.CONFIRM;
    final AtomicInteger submits = new AtomicInteger();
    final AtomicReference<String> protocol = new AtomicReference<>();
    final java.util.concurrent.ConcurrentHashMap<String, String> byIdempotency =
        new java.util.concurrent.ConcurrentHashMap<>();
    final AtomicInteger dropRemaining = new AtomicInteger(0);

    void reset() {
      mode = Mode.CONFIRM;
      submits.set(0);
      protocol.set(null);
      byIdempotency.clear();
      dropRemaining.set(0);
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
      String existing = byIdempotency.get(idempotencyKey);
      if (existing != null) {
        protocol.set(existing);
        return existing;
      }
      String p = "prot-" + UUID.randomUUID();
      byIdempotency.put(idempotencyKey, p);
      protocol.set(p);
      if (mode == Mode.LOST_FIRST_RESPONSE && dropRemaining.get() > 0) {
        dropRemaining.decrementAndGet();
        // First acceptance is durable at Central, but response is lost in transit.
        throw new CentralException("simulated lost response after accept", true, 0);
      }
      return p;
    }

    @Override
    public CentralStatus pollStatus(String protocol, String correlationId) {
      if (mode == Mode.CONFIRM || mode == Mode.LOST_FIRST_RESPONSE) {
        return CentralStatus.terminal("CONFIRMADA", null);
      }
      if (mode == Mode.REJECT) {
        return CentralStatus.terminal("REJEITADA", "Simulated rejection");
      }
      throw new CentralException("simulated 503", true, 503);
    }
  }
}
