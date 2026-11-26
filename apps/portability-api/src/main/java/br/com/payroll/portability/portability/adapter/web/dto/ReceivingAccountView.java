package br.com.payroll.portability.portability.adapter.web.dto;

import br.com.payroll.portability.portability.domain.ReceivingAccount;

/** API view of destination account with conta masked. */
public record ReceivingAccountView(String banco, String agencia, String conta) {

  public static ReceivingAccountView masked(ReceivingAccount account) {
    return new ReceivingAccountView(account.bank(), account.agency(), account.redactedAccount());
  }
}
