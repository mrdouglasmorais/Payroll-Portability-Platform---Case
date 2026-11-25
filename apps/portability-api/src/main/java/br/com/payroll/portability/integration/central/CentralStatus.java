package br.com.payroll.portability.integration.central;

public record CentralStatus(boolean awaiting, String status, String motivoRejeicao) {

  public static CentralStatus pending() {
    return new CentralStatus(true, null, null);
  }

  public static CentralStatus terminal(String status, String motivoRejeicao) {
    return new CentralStatus(false, status, motivoRejeicao);
  }
}
