package br.com.payroll.portability.schedule.domain;

import br.com.payroll.portability.portability.domain.ReceivingAccount;
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
  /** Conta vigente / efetivada — só muda após confirmação da portabilidade. */
  private ReceivingAccount effectiveAccount;

  public InstallmentSnapshot(
      UUID id,
      String externalInstallmentId,
      UUID beneficiaryId,
      String agreementId,
      Money amount,
      LocalDate discountDate,
      InstallmentStatus status) {
    this(
        id,
        externalInstallmentId,
        beneficiaryId,
        agreementId,
        amount,
        discountDate,
        status,
        new ReceivingAccount("000", "0001", "00000-0"));
  }

  public InstallmentSnapshot(
      UUID id,
      String externalInstallmentId,
      UUID beneficiaryId,
      String agreementId,
      Money amount,
      LocalDate discountDate,
      InstallmentStatus status,
      ReceivingAccount effectiveAccount) {
    this.id = Objects.requireNonNull(id);
    this.externalInstallmentId = Objects.requireNonNull(externalInstallmentId);
    this.beneficiaryId = Objects.requireNonNull(beneficiaryId);
    this.agreementId = Objects.requireNonNull(agreementId);
    this.amount = Objects.requireNonNull(amount);
    this.discountDate = Objects.requireNonNull(discountDate);
    this.status = Objects.requireNonNull(status);
    this.effectiveAccount = Objects.requireNonNull(effectiveAccount);
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

  public void markPorted(ReceivingAccount confirmedDestination) {
    if (status != InstallmentStatus.RESERVED) {
      throw new DomainException("Only reserved installments can be marked ported");
    }
    this.status = InstallmentStatus.PORTED;
    this.effectiveAccount = Objects.requireNonNull(confirmedDestination);
  }

  /** Refresh agenda attributes for OPEN rows without touching the effective account. */
  public InstallmentSnapshot withOpenRefresh(
      String agreementId, Money amount, LocalDate discountDate) {
    if (status != InstallmentStatus.OPEN) {
      return this;
    }
    return new InstallmentSnapshot(
        id,
        externalInstallmentId,
        beneficiaryId,
        Objects.requireNonNull(agreementId),
        Objects.requireNonNull(amount),
        Objects.requireNonNull(discountDate),
        status,
        effectiveAccount);
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

  public ReceivingAccount effectiveAccount() {
    return effectiveAccount;
  }
}
