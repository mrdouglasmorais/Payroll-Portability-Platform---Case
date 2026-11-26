package br.com.payroll.central.application;

import java.util.Random;

/**
 * Resolves Central fault injection mode.
 *
 * <p>Precedence (deterministic first for CI):
 *
 * <ol>
 *   <li>{@code X-Sim-Fault} header
 *   <li>Beneficiary id suffix ({@code -reject}, {@code -503}, {@code -timeout})
 *   <li>Optional probabilistic rate ({@code CENTRAL_FAULT_RATE}) with seeded RNG
 * </ol>
 *
 * Default rate is {@code 0} so CI and dual-stack remain fully deterministic.
 */
public final class FaultSelector {

  private final double faultRate;
  private final Random random;

  public FaultSelector(double faultRate, long seed) {
    if (faultRate < 0.0 || faultRate > 1.0) {
      throw new IllegalArgumentException("faultRate must be between 0 and 1");
    }
    this.faultRate = faultRate;
    this.random = new Random(seed);
  }

  public FaultMode select(String faultHeader, String beneficiaryId) {
    if (faultHeader != null && !faultHeader.isBlank()) {
      return FaultMode.valueOf(faultHeader.trim().toUpperCase());
    }
    if (beneficiaryId != null) {
      if (beneficiaryId.endsWith("-timeout")) {
        return FaultMode.TIMEOUT;
      }
      if (beneficiaryId.endsWith("-reject")) {
        return FaultMode.REJECT;
      }
      if (beneficiaryId.endsWith("-503")) {
        return FaultMode.UNAVAILABLE;
      }
    }
    if (faultRate > 0.0 && random.nextDouble() < faultRate) {
      int pick = random.nextInt(3);
      return switch (pick) {
        case 0 -> FaultMode.REJECT;
        case 1 -> FaultMode.TIMEOUT;
        default -> FaultMode.UNAVAILABLE;
      };
    }
    return FaultMode.CONFIRM;
  }
}
