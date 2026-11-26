package br.com.payroll.portability.integration.central;

import java.time.Duration;

public class CentralException extends RuntimeException {

  private final boolean transientFailure;
  private final int statusCode;
  private final Duration retryAfter;
  private final IntegrationFailureCode failureCode;

  public CentralException(String message, boolean transientFailure, int statusCode) {
    this(message, transientFailure, statusCode, null, null, null);
  }

  public CentralException(
      String message, boolean transientFailure, int statusCode, Throwable cause) {
    this(message, transientFailure, statusCode, null, null, cause);
  }

  public CentralException(
      String message,
      boolean transientFailure,
      int statusCode,
      Duration retryAfter,
      Throwable cause) {
    this(message, transientFailure, statusCode, retryAfter, null, cause);
  }

  public CentralException(
      String message,
      boolean transientFailure,
      int statusCode,
      Duration retryAfter,
      IntegrationFailureCode failureCode,
      Throwable cause) {
    super(sanitize(message), cause);
    this.transientFailure = transientFailure;
    this.statusCode = statusCode;
    this.retryAfter = retryAfter;
    this.failureCode =
        failureCode != null ? failureCode : inferCode(transientFailure, statusCode);
  }

  public boolean isTransientFailure() {
    return transientFailure;
  }

  public int statusCode() {
    return statusCode;
  }

  public Duration retryAfter() {
    return retryAfter;
  }

  public IntegrationFailureCode failureCode() {
    return failureCode;
  }

  /** Permanent client errors that should not retry indefinitely as if ambiguous. */
  public boolean isPermanentFailure() {
    return !transientFailure && statusCode >= 400 && statusCode < 500 && statusCode != 429;
  }

  private static IntegrationFailureCode inferCode(boolean transientFailure, int statusCode) {
    if (statusCode == 429) {
      return IntegrationFailureCode.CENTRAL_RATE_LIMITED;
    }
    if (statusCode == 409) {
      return IntegrationFailureCode.CENTRAL_CONFLICT;
    }
    if (statusCode >= 500) {
      return IntegrationFailureCode.CENTRAL_UNAVAILABLE;
    }
    if (statusCode == 0) {
      return IntegrationFailureCode.TRANSPORT_FAILURE;
    }
    if (!transientFailure && statusCode >= 400) {
      return IntegrationFailureCode.CENTRAL_CLIENT_ERROR;
    }
    return IntegrationFailureCode.INTEGRATION_ERROR;
  }

  static String sanitize(String message) {
    if (message == null) {
      return "central error";
    }
    String trimmed = message.replaceAll("[\\r\\n]+", " ").trim();
    if (trimmed.length() > 240) {
      return trimmed.substring(0, 240);
    }
    return trimmed;
  }
}
