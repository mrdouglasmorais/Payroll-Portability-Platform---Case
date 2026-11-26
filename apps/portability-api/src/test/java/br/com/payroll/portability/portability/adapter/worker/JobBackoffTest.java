package br.com.payroll.portability.portability.adapter.worker;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JobBackoffTest {

  @Test
  void delayIsBoundedByMax() {
    Duration delay = JobBackoff.nextDelay(Duration.ofMillis(10), 20, Duration.ofMillis(100));
    assertTrue(delay.toMillis() <= 100);
    assertTrue(delay.toMillis() >= 0);
  }
}
