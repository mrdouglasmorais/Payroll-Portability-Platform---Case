package br.com.payroll.portability.integration.central;

import java.time.Duration;

public record CentralStatus(
    boolean awaiting, String status, String motivoRejeicao, Duration retryAfter) {

  public static CentralStatus pending() {
    return new CentralStatus(true, null, null, null);
  }

  public static CentralStatus pending(Duration retryAfter) {
    return new CentralStatus(true, null, null, retryAfter);
  }

  public static CentralStatus terminal(String status, String motivoRejeicao) {
    return new CentralStatus(false, status, motivoRejeicao, null);
  }
}
