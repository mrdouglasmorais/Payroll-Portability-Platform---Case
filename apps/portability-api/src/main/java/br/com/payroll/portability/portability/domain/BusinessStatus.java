package br.com.payroll.portability.portability.domain;

public enum BusinessStatus {
  PENDENTE,
  CONFIRMADA,
  REJEITADA;

  public boolean isTerminal() {
    return this == CONFIRMADA || this == REJEITADA;
  }

  public boolean canTransitionTo(BusinessStatus next) {
    if (this.isTerminal()) {
      return false;
    }
    return next == CONFIRMADA || next == REJEITADA;
  }
}
