package br.com.payroll.portability.portability.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.payroll.portability.consent.application.ConsentRepository;
import br.com.payroll.portability.consent.domain.Consent;
import br.com.payroll.portability.portability.domain.PortabilityCommand;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import br.com.payroll.portability.schedule.domain.InstallmentStatus;
import br.com.payroll.portability.shared.domain.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CreatePortabilityServiceTest {

  @Container
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("portability")
          .withUsername("portability")
          .withPassword("portability");

  @DynamicPropertySource
  static void datasourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("app.central.worker-enabled", () -> "false");
  }

  @Autowired CreatePortabilityService service;
  @Autowired ConsentRepository consentRepository;
  @Autowired InstallmentRepository installmentRepository;
  @Autowired PortabilityOperationRepository operationRepository;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;

  private UUID beneficiaryId;

  @BeforeEach
  void setUp() {
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
            "parcela-1",
            beneficiaryId,
            "conv-1",
            Money.of("100.00"),
            LocalDate.of(2026, 12, 10),
            InstallmentStatus.OPEN));
    installmentRepository.save(
        new InstallmentSnapshot(
            UUID.randomUUID(),
            "parcela-2",
            beneficiaryId,
            "conv-1",
            Money.of("200.00"),
            LocalDate.of(2027, 1, 10),
            InstallmentStatus.OPEN));
  }

  @Test
  void createsOperationReservationEventAndJobAtomically() {
    UUID operationId =
        service.create(
            new PortabilityCommand(
                beneficiaryId,
                Set.of("parcela-1"),
                "creditor-x",
                new ReceivingAccount("001", "1234", "56789-0"),
                "idem-1",
                "corr-1",
                "hash-a"));

    assertTrue(operationRepository.findById(operationId).isPresent());
    Integer reservations =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM installment_reservations WHERE status = 'ACTIVE'", Integer.class);
    Integer events =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM operation_events WHERE operation_id = ?",
            Integer.class,
            operationId);
    Integer jobs =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM processing_jobs WHERE operation_id = ?",
            Integer.class,
            operationId);
    assertEquals(1, reservations);
    assertEquals(1, events);
    assertEquals(1, jobs);
  }

  @Test
  void sameIdempotencyKeyReturnsSameOperation() {
    PortabilityCommand command =
        new PortabilityCommand(
            beneficiaryId,
            Set.of("parcela-1"),
            "creditor-x",
            new ReceivingAccount("001", "1234", "56789-0"),
            "idem-same",
            "corr-1",
            "hash-a");
    UUID first = service.create(command);
    UUID second = service.create(command);
    assertEquals(first, second);
  }

  @Test
  void mismatchedIdempotencyPayloadConflicts() {
    service.create(
        new PortabilityCommand(
            beneficiaryId,
            Set.of("parcela-1"),
            "creditor-x",
            new ReceivingAccount("001", "1234", "56789-0"),
            "idem-conflict",
            "corr-1",
            "hash-a"));
    assertThrows(
        IdempotencyConflictException.class,
        () ->
            service.create(
                new PortabilityCommand(
                    beneficiaryId,
                    Set.of("parcela-2"),
                    "creditor-x",
                    new ReceivingAccount("001", "1234", "56789-0"),
                    "idem-conflict",
                    "corr-2",
                    "hash-b")));
  }

  @Test
  void concurrentReservationsAllowOnlyOneWinner() throws Exception {
    var latch = new CountDownLatch(1);
    var successes = new AtomicInteger();
    var failures = new AtomicInteger();
    try (var executor = Executors.newFixedThreadPool(2)) {
      Future<?> a =
          executor.submit(
              () ->
                  compete(
                      latch,
                      successes,
                      failures,
                      "idem-c1",
                      "hash-c1"));
      Future<?> b =
          executor.submit(
              () ->
                  compete(
                      latch,
                      successes,
                      failures,
                      "idem-c2",
                      "hash-c2"));
      latch.countDown();
      a.get();
      b.get();
    }
    assertEquals(1, successes.get());
    assertEquals(1, failures.get());
    Integer active =
        jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM installment_reservations
            WHERE external_installment_id = 'parcela-1' AND status = 'ACTIVE'
            """,
            Integer.class);
    assertEquals(1, active);
  }

  private void compete(
      CountDownLatch latch,
      AtomicInteger successes,
      AtomicInteger failures,
      String idempotencyKey,
      String hash) {
    try {
      latch.await();
      service.create(
          new PortabilityCommand(
              beneficiaryId,
              Set.of("parcela-1"),
              "creditor-x",
              new ReceivingAccount("001", "1234", "56789-0"),
              idempotencyKey,
              UUID.randomUUID().toString(),
              hash));
      successes.incrementAndGet();
    } catch (Exception ex) {
      failures.incrementAndGet();
    }
  }
}
