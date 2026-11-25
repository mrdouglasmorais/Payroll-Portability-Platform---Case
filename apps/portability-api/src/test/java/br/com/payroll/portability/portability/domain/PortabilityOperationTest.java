package br.com.payroll.portability.portability.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.payroll.portability.shared.domain.DomainException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortabilityOperationTest {

  private final Instant now = Instant.parse("2026-11-25T20:00:00Z");

  @Test
  void createsPendingOperationWithAuditEvent() {
    PortabilityOperation operation = newOperation();
    assertEquals(BusinessStatus.PENDENTE, operation.businessStatus());
    assertEquals(1, operation.pullNewEvents().size());
  }

  @Test
  void rejectsDuplicateInstallments() {
    assertThrows(
        DomainException.class,
        () ->
            PortabilityOperation.create(
                UUID.randomUUID(),
                "creditor-a",
                new ReceivingAccount("001", "1234", "99999"),
                List.of("parcela-1", "parcela-1"),
                "corr-1",
                now));
  }

  @Test
  void terminalStatesCannotBeReverted() {
    PortabilityOperation operation = newOperation();
    operation.confirm(now);
    assertThrows(DomainException.class, () -> operation.reject("late", now));
  }

  @Test
  void rejectionRequiresReason() {
    PortabilityOperation operation = newOperation();
    assertThrows(DomainException.class, () -> operation.reject(" ", now));
  }

  private PortabilityOperation newOperation() {
    return PortabilityOperation.create(
        UUID.randomUUID(),
        "creditor-a",
        new ReceivingAccount("001", "1234", "99999"),
        Set.of("parcela-1", "parcela-2"),
        "corr-1",
        now);
  }
}
