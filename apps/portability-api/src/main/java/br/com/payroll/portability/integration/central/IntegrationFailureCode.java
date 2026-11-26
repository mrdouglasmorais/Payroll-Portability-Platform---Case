package br.com.payroll.portability.integration.central;

public enum IntegrationFailureCode {
  CENTRAL_UNAVAILABLE,
  CENTRAL_RATE_LIMITED,
  CENTRAL_CLIENT_ERROR,
  CENTRAL_CONFLICT,
  HMAC_FAILURE,
  TRANSPORT_FAILURE,
  POLL_DEADLINE_EXCEEDED,
  PERMANENT_CENTRAL_ERROR,
  INTEGRATION_ERROR;

  public static IntegrationFailureCode fromCentral(CentralException ex) {
    int code = ex.statusCode();
    String message = ex.getMessage() == null ? "" : ex.getMessage().toUpperCase();
    if (message.contains("SIGNATURE") || message.contains("HMAC")) {
      return HMAC_FAILURE;
    }
    if (code == 429) {
      return CENTRAL_RATE_LIMITED;
    }
    if (code == 409) {
      return CENTRAL_CONFLICT;
    }
    if (code >= 500 || code == 0) {
      return code == 0 ? TRANSPORT_FAILURE : CENTRAL_UNAVAILABLE;
    }
    if (code >= 400) {
      return CENTRAL_CLIENT_ERROR;
    }
    return INTEGRATION_ERROR;
  }
}
