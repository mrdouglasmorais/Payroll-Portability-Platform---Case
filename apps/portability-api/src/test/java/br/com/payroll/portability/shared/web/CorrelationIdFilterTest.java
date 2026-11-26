package br.com.payroll.portability.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CorrelationIdFilterTest {

  @Test
  void blankIncomingGeneratesUuid() {
    String generated = CorrelationIdFilter.sanitize("  ");
    assertTrue(generated.length() >= 32);
  }

  @Test
  void longIncomingIsTruncated() {
    String oversized = "c".repeat(200);
    assertEquals(128, CorrelationIdFilter.sanitize(oversized).length());
  }

  @Test
  void validIncomingIsPreserved() {
    assertEquals("corr-abc", CorrelationIdFilter.sanitize("corr-abc"));
  }
}
