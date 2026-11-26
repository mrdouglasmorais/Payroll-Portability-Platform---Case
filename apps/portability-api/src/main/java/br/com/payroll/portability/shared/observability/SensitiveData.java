package br.com.payroll.portability.shared.observability;

/** Masks bank routing details for operational logs. Never log full account/agency. */
public final class SensitiveData {

  private SensitiveData() {}

  public static String maskAccount(String account) {
    return maskTail(account, 4);
  }

  public static String maskAgency(String agency) {
    return maskTail(agency, 2);
  }

  private static String maskTail(String value, int visibleTail) {
    if (value == null || value.isBlank()) {
      return "****";
    }
    String trimmed = value.trim();
    if (trimmed.length() <= visibleTail) {
      return "****";
    }
    return "****" + trimmed.substring(trimmed.length() - visibleTail);
  }
}
