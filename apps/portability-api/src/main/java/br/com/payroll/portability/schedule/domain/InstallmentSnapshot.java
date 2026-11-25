package br.com.payroll.portability.schedule.domain;

import br.com.payroll.portability.shared.domain.DomainException;
import br.com.payroll.portability.shared.domain.Money;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class InstallmentSnapshot {

  private final UUID id;
  private final String externalInstallmentId;
  private final UUID beneficiaryId;
  private final String agreementId;
  private final Money amount;
  private final LocalDate discountDate;
  private InstallmentStatus status;

  public InstallmentSnapshot(
      UUID id,
      String externalInstallmentId,
      UUID beneficiaryId,
      String agreementId,
      Money amount,
      LocalDate discountDate,
      InstallmentStatus status) {
    this.id = Objects.requireNonNull(id);
    this.externalInstallmentId = Objects.requireNonNull(externalInstallmentId);
    this.beneficiaryId = Objects.requireNonNull(beneficiaryId);
    this.agreementId = Objects.requireNonNull(agreementId);
    this.amount = Objects.requireNonNull(amount);
    this.discountDate = Objects.requireNonNull(discountDate);
    this.status = Objects.requireNonNull(status);
  }

  public void reserve() {
    if (!status.isReservable()) {
      throw new DomainException("Installment is not reservable: " + status);
    }
    this.status = InstallmentStatus.RESERVED;
  }

  public void release() {
    if (status != InstallmentStatus.RESERVED) {
      throw new DomainException("Only reserved installments can be released");
    }
    this.status = InstallmentStatus.OPEN;
  }

  public void markPorted() {
    if (status != InstallmentStatus.RESERVED) {
      throw new DomainException("Only reserved installments can be marked ported");
    }
    this.status = InstallmentStatus.PORTED;
  }

  public UUID id() {
    return id;
  }

  public String externalInstallmentId() {
    return externalInstallmentId;
  }

  public UUID beneficiaryId() {
    return beneficiaryId;
  }

  public String agreementId() {
    return agreementId;
  }

  public Money amount() {
    return amount;
  }

  public LocalDate discountDate() {
    return discountDate;
  }

  public InstallmentStatus status() {
    return status;
  }
}
