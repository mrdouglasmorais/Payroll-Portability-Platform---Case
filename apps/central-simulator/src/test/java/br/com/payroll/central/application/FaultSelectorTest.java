package br.com.payroll.central.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FaultSelectorTest {

  @Test
  void headerOverridesSuffixAndRate() {
    FaultSelector selector = new FaultSelector(1.0, 1L);
    assertEquals(FaultMode.TIMEOUT, selector.select("TIMEOUT", "ben-reject"));
  }

  @Test
  void invalidHeaderThrowsInsteadOfCrashingUnchecked() {
    FaultSelector selector = new FaultSelector(0.0, 1L);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> selector.select("BOOM", "ben-001"));
    assertTrue(ex.getMessage().contains("Invalid X-Sim-Fault"));
  }

  @Test
  void suffixIsDeterministicWhenRateZero() {
    FaultSelector selector = new FaultSelector(0.0, 99L);
    assertEquals(FaultMode.REJECT, selector.select(null, "ben-001-reject"));
    assertEquals(FaultMode.UNAVAILABLE, selector.select(null, "ben-001-503"));
    assertEquals(FaultMode.TIMEOUT, selector.select(null, "ben-001-timeout"));
    assertEquals(FaultMode.CONFIRM, selector.select(null, "ben-001"));
  }

  @Test
  void sameSeedProducesSameProbabilisticSequence() {
    FaultSelector a = new FaultSelector(1.0, 7L);
    FaultSelector b = new FaultSelector(1.0, 7L);
    for (int i = 0; i < 20; i++) {
      assertEquals(a.select(null, "ben-demo"), b.select(null, "ben-demo"));
    }
  }

  @Test
  void probabilisticModeEmitsFaultsWithFullRate() {
    FaultSelector selector = new FaultSelector(1.0, 123L);
    Set<FaultMode> seen = new HashSet<>();
    for (int i = 0; i < 30; i++) {
      FaultMode mode = selector.select(null, "ben-demo");
      assertNotEquals(FaultMode.CONFIRM, mode);
      seen.add(mode);
    }
    assertTrue(seen.containsAll(EnumSet.of(FaultMode.REJECT, FaultMode.TIMEOUT, FaultMode.UNAVAILABLE)));
  }
}
