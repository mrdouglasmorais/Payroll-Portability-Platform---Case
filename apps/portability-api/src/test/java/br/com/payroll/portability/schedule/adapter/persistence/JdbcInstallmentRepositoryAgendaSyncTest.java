package br.com.payroll.portability.schedule.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.payroll.portability.portability.domain.ReceivingAccount;
import br.com.payroll.portability.schedule.application.InstallmentRepository;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import br.com.payroll.portability.schedule.domain.InstallmentStatus;
import br.com.payroll.portability.shared.domain.Money;
import java.time.LocalDate;
import java.util.UUID;
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
class JdbcInstallmentRepositoryAgendaSyncTest {

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

  @Autowired InstallmentRepository installmentRepository;
  @Autowired JdbcTemplate jdbc;

  private UUID beneficiaryId;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM installment_reservations");
    jdbc.update("DELETE FROM operation_installments");
    jdbc.update("DELETE FROM installment_snapshots");
    jdbc.update("DELETE FROM beneficiaries");

    beneficiaryId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO beneficiaries (id, external_ref, display_name) VALUES (?, ?, ?)",
        beneficiaryId,
        "ben-agenda-sync",
        "Agenda Sync Beneficiary");
  }

  @Test
  void updateOpenAgendaAttributesDoesNotTouchReservedOrPorted() {
    ReceivingAccount original = new ReceivingAccount("104", "4321", "11111-1");

    InstallmentSnapshot reserved =
        new InstallmentSnapshot(
            UUID.randomUUID(),
            "parcela-reserved",
            beneficiaryId,
            "conv-1",
            Money.of("10.00"),
            LocalDate.of(2026, 12, 15),
            InstallmentStatus.OPEN,
            original);
    reserved.reserve();
    installmentRepository.save(reserved);

    InstallmentSnapshot ported =
        new InstallmentSnapshot(
            UUID.randomUUID(),
            "parcela-ported",
            beneficiaryId,
            "conv-1",
            Money.of("20.00"),
            LocalDate.of(2027, 1, 15),
            InstallmentStatus.OPEN,
            original);
    ported.reserve();
    ported.markPorted(new ReceivingAccount("001", "1234", "99999-9"));
    installmentRepository.save(ported);

    InstallmentSnapshot open =
        new InstallmentSnapshot(
            UUID.randomUUID(),
            "parcela-open",
            beneficiaryId,
            "conv-1",
            Money.of("30.00"),
            LocalDate.of(2027, 2, 15),
            InstallmentStatus.OPEN,
            original);
    installmentRepository.save(open);

    int reservedUpdated =
        installmentRepository.updateOpenAgendaAttributes(
            reserved.withOpenRefresh("conv-2", Money.of("99.00"), LocalDate.of(2030, 1, 1)));
    int portedUpdated =
        installmentRepository.updateOpenAgendaAttributes(
            ported.withOpenRefresh("conv-2", Money.of("99.00"), LocalDate.of(2030, 1, 1)));
    int openUpdated =
        installmentRepository.updateOpenAgendaAttributes(
            open.withOpenRefresh("conv-2", Money.of("31.00"), LocalDate.of(2027, 3, 15)));

    assertEquals(0, reservedUpdated);
    assertEquals(0, portedUpdated);
    assertEquals(1, openUpdated);

    InstallmentSnapshot reservedAfter =
        installmentRepository.findByExternalId("parcela-reserved").orElseThrow();
    InstallmentSnapshot portedAfter =
        installmentRepository.findByExternalId("parcela-ported").orElseThrow();
    InstallmentSnapshot openAfter =
        installmentRepository.findByExternalId("parcela-open").orElseThrow();

    assertEquals(InstallmentStatus.RESERVED, reservedAfter.status());
    assertEquals("104", reservedAfter.effectiveAccount().bank());
    assertEquals(InstallmentStatus.PORTED, portedAfter.status());
    assertEquals("001", portedAfter.effectiveAccount().bank());
    assertEquals("99999-9", portedAfter.effectiveAccount().account());
    assertEquals(InstallmentStatus.OPEN, openAfter.status());
    assertEquals("conv-2", openAfter.agreementId());
    assertEquals(Money.of("31.00"), openAfter.amount());
    assertEquals(LocalDate.of(2027, 3, 15), openAfter.discountDate());
  }

  @Test
  void findByExternalIdsReturnsSortedByExternalId() {
    installmentRepository.save(
        new InstallmentSnapshot(
            UUID.randomUUID(),
            "parcela-z",
            beneficiaryId,
            "conv-1",
            Money.of("1.00"),
            LocalDate.of(2026, 12, 1),
            InstallmentStatus.OPEN));
    installmentRepository.save(
        new InstallmentSnapshot(
            UUID.randomUUID(),
            "parcela-a",
            beneficiaryId,
            "conv-1",
            Money.of("2.00"),
            LocalDate.of(2026, 12, 2),
            InstallmentStatus.OPEN));

    var found =
        installmentRepository.findByExternalIds(java.util.List.of("parcela-z", "parcela-a"));
    assertEquals("parcela-a", found.get(0).externalInstallmentId());
    assertEquals("parcela-z", found.get(1).externalInstallmentId());
  }
}
