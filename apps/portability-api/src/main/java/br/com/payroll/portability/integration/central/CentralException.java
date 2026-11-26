package br.com.payroll.portability.integration.central;

import java.time.Duration;

public class CentralException extends RuntimeException {

  private final boolean transientFailure;
  private final int statusCode;
  private final Duration retryAfter;

  public CentralException(String message, boolean transientFailure, int statusCode) {
    this(message, transientFailure, statusCode, null, null);
  }

  public CentralException(
      String message, boolean transientFailure, int statusCode, Throwable cause) {
    this(message, transientFailure, statusCode, null, cause);
  }

  public CentralException(
      String message,
      boolean transientFailure,
      int statusCode,
      Duration retryAfter,
      Throwable cause) {
    super(sanitize(message), cause);
    this.transientFailure = transientFailure;
    this.statusCode = statusCode;
    this.retryAfter = retryAfter;
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

  /** Permanent client errors that should not retry indefinitely as if ambiguous. */
  public boolean isPermanentFailure() {
    return !transientFailure && statusCode >= 400 && statusCode < 500 && statusCode != 429;
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
