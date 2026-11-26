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
    return ex.failureCode();
  }
}
