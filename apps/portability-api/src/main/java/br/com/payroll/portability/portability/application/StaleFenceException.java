package br.com.payroll.portability.portability.application;

public class StaleFenceException extends RuntimeException {

  public StaleFenceException(String message) {
    super(message);
  }
}
