package br.com.payroll.portability.integration.central;

public class CentralException extends RuntimeException {

  private final boolean transientFailure;
  private final int statusCode;

  public CentralException(String message, boolean transientFailure, int statusCode) {
    super(message);
    this.transientFailure = transientFailure;
    this.statusCode = statusCode;
  }

  public CentralException(String message, boolean transientFailure, int statusCode, Throwable cause) {
    super(message, cause);
    this.transientFailure = transientFailure;
    this.statusCode = statusCode;
  }

  public boolean isTransientFailure() {
    return transientFailure;
  }

  public int statusCode() {
    return statusCode;
  }
}
