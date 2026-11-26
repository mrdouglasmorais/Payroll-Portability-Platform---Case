package br.com.payroll.portability.shared.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PortabilityTelemetryCommitTest {

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

  @Autowired PortabilityTelemetry telemetry;
  @Autowired MeterRegistry meterRegistry;
  @Autowired PlatformTransactionManager transactionManager;

  private TransactionTemplate tx;

  @BeforeEach
  void setUp() {
    tx = new TransactionTemplate(transactionManager);
  }

  @Test
  void financialCountersIncrementOnlyAfterSuccessfulCommit() {
    double createdBefore = count("created");
    double confirmedBefore = count("confirmed");
    double rejectedBefore = count("rejected");

    tx.executeWithoutResult(
        status -> {
          telemetry.operationCreated();
          telemetry.operationConfirmed();
          telemetry.operationRejected();
          assertEquals(createdBefore, count("created"));
          assertEquals(confirmedBefore, count("confirmed"));
          assertEquals(rejectedBefore, count("rejected"));
        });

    assertEquals(createdBefore + 1.0, count("created"));
    assertEquals(confirmedBefore + 1.0, count("confirmed"));
    assertEquals(rejectedBefore + 1.0, count("rejected"));
  }

  @Test
  void financialCountersDoNotIncrementWhenTransactionRollsBack() {
    double createdBefore = count("created");
    double confirmedBefore = count("confirmed");
    double rejectedBefore = count("rejected");

    assertThrows(
        IllegalStateException.class,
        () ->
            tx.executeWithoutResult(
                status -> {
                  telemetry.operationCreated();
                  telemetry.operationConfirmed();
                  telemetry.operationRejected();
                  throw new IllegalStateException("forced rollback");
                }));

    assertEquals(createdBefore, count("created"));
    assertEquals(confirmedBefore, count("confirmed"));
    assertEquals(rejectedBefore, count("rejected"));
  }

  private double count(String outcome) {
    return meterRegistry
        .counter("portability_operations_total", "outcome", outcome)
        .count();
  }
}
