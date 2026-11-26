package br.com.payroll.portability.shared.domain;

/** Competing create / unavailable installment — always mapped to HTTP 409. */
public final class InstallmentConflictException extends RuntimeException {

  public enum Reason {
    ALREADY_RESERVED,
    ALREADY_PORTED,
    UNAVAILABLE
  }

  private final Reason reason;
  private final String externalInstallmentId;

  private InstallmentConflictException(Reason reason, String externalInstallmentId) {
    super(messageFor(reason, externalInstallmentId));
    this.reason = reason;
    this.externalInstallmentId = externalInstallmentId;
  }

  public static InstallmentConflictException alreadyReserved(String externalInstallmentId) {
    return new InstallmentConflictException(Reason.ALREADY_RESERVED, externalInstallmentId);
  }

  public static InstallmentConflictException alreadyPorted(String externalInstallmentId) {
    return new InstallmentConflictException(Reason.ALREADY_PORTED, externalInstallmentId);
  }

  public static InstallmentConflictException unavailable(String externalInstallmentId) {
    return new InstallmentConflictException(Reason.UNAVAILABLE, externalInstallmentId);
  }

  public Reason reason() {
    return reason;
  }

  public String externalInstallmentId() {
    return externalInstallmentId;
  }

  private static String messageFor(Reason reason, String externalId) {
    return switch (reason) {
      case ALREADY_PORTED -> "Parcela já portada: " + externalId;
      case ALREADY_RESERVED -> "Parcela já reservada: " + externalId;
      case UNAVAILABLE -> "Parcela indisponível: " + externalId;
    };
  }
}
