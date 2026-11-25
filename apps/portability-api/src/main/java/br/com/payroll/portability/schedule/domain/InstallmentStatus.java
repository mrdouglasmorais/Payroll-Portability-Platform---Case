package br.com.payroll.portability.schedule.domain;

public enum InstallmentStatus {
  OPEN,
  RESERVED,
  PORTED,
  CLOSED;

  public boolean isReservable() {
    return this == OPEN;
  }
}
