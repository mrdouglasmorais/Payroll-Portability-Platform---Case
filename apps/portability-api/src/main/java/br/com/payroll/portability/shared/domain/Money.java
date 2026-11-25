package br.com.payroll.portability.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Monetary amount with fixed scale 2. Never use binary floating point. */
public final class Money {

  private final BigDecimal amount;

  private Money(BigDecimal amount) {
    this.amount = amount;
  }

  public static Money of(BigDecimal value) {
    Objects.requireNonNull(value, "amount");
    if (value.scale() > 2) {
      throw new IllegalArgumentException("Money scale must be at most 2");
    }
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Money cannot be negative");
    }
    return new Money(value.setScale(2, RoundingMode.UNNECESSARY));
  }

  public static Money of(String value) {
    return of(new BigDecimal(value));
  }

  public BigDecimal amount() {
    return amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Money money)) {
      return false;
    }
    return amount.compareTo(money.amount) == 0;
  }

  @Override
  public int hashCode() {
    return amount.stripTrailingZeros().hashCode();
  }

  @Override
  public String toString() {
    return amount.toPlainString();
  }
}
