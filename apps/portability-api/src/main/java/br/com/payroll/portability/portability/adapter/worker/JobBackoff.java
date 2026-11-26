package br.com.payroll.portability.portability.adapter.worker;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/** Exponential backoff with full jitter, capped. */
public final class JobBackoff {

  private JobBackoff() {}

  public static Duration nextDelay(Duration base, int attemptCount, Duration max) {
    int attempt = Math.max(1, attemptCount);
    long baseMs = Math.max(1L, base.toMillis());
    long exp = baseMs * (1L << Math.min(attempt - 1, 10));
    long capped = Math.min(exp, Math.max(baseMs, max.toMillis()));
    long jittered = ThreadLocalRandom.current().nextLong(0, capped + 1);
    return Duration.ofMillis(Math.max(baseMs / 4, jittered));
  }
}
