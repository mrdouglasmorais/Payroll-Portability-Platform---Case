package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.integration.integrity.HmacCanonical;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;

public final class RequestHash {

  private RequestHash() {}

  public static String forPortability(
      String beneficiaryExternalRef,
      Collection<String> parcelas,
      String creditor,
      ReceivingAccount account) {
    String sorted =
        parcelas.stream().sorted().collect(Collectors.joining(","));
    String canonical =
        String.join(
            "|",
            nullToEmpty(beneficiaryExternalRef),
            sorted,
            nullToEmpty(creditor),
            nullToEmpty(account.bank()),
            nullToEmpty(account.agency()),
            nullToEmpty(account.account()));
    return HmacCanonical.sha256Hex(canonical.getBytes(StandardCharsets.UTF_8));
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
