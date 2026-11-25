package br.com.payroll.portability.shared.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

  @Test
  void acceptsFixedScaleAmounts() {
    assertEquals(new BigDecimal("10.50"), Money.of("10.50").amount());
  }

  @Test
  void rejectsNegativeAmounts() {
    assertThrows(IllegalArgumentException.class, () -> Money.of("-0.01"));
  }

  @Test
  void rejectsExcessiveScale() {
    assertThrows(IllegalArgumentException.class, () -> Money.of(new BigDecimal("1.234")));
  }
}
