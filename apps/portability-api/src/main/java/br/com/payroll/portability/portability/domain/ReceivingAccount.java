package br.com.payroll.portability.portability.domain;

import java.util.Objects;

public record ReceivingAccount(String bank, String agency, String account) {

  public ReceivingAccount {
    Objects.requireNonNull(bank, "bank");
    Objects.requireNonNull(agency, "agency");
    Objects.requireNonNull(account, "account");
    if (bank.isBlank() || agency.isBlank() || account.isBlank()) {
      throw new IllegalArgumentException("Receiving account fields must not be blank");
    }
  }

  public String redactedAccount() {
    if (account.length() <= 4) {
      return "****";
    }
    return "****" + account.substring(account.length() - 4);
  }
}
