package br.com.payroll.portability.consent.domain;

import br.com.payroll.portability.shared.domain.DomainException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Consent {

  private final UUID id;
  private final UUID beneficiaryId;
  private final String agreementId;
  private String authorizationId;
  private ConsentStatus status;
  private final Instant createdAt;
  private Instant updatedAt;

  private Consent(
      UUID id,
      UUID beneficiaryId,
      String agreementId,
      String authorizationId,
      ConsentStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.beneficiaryId = beneficiaryId;
    this.agreementId = agreementId;
    this.authorizationId = authorizationId;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Consent request(UUID beneficiaryId, String agreementId, Instant now) {
    Objects.requireNonNull(beneficiaryId, "beneficiaryId");
    Objects.requireNonNull(agreementId, "agreementId");
    Objects.requireNonNull(now, "now");
    if (agreementId.isBlank()) {
      throw new DomainException("Agreement id is required");
    }
    return new Consent(
        UUID.randomUUID(), beneficiaryId, agreementId, null, ConsentStatus.PENDING, now, now);
  }

  public static Consent reconstitute(
      UUID id,
      UUID beneficiaryId,
      String agreementId,
      String authorizationId,
      ConsentStatus status,
      Instant createdAt,
      Instant updatedAt) {
    return new Consent(
        id, beneficiaryId, agreementId, authorizationId, status, createdAt, updatedAt);
  }

  public void accept(String authorizationId, Instant now) {
    if (status != ConsentStatus.PENDING) {
      throw new DomainException("Only pending consent can be accepted");
    }
    if (authorizationId == null || authorizationId.isBlank()) {
      throw new DomainException("Authorization id is required after Central acceptance");
    }
    this.authorizationId = authorizationId;
    this.status = ConsentStatus.ACCEPTED;
    this.updatedAt = now;
  }

  public void reject(Instant now) {
    if (status != ConsentStatus.PENDING) {
      throw new DomainException("Only pending consent can be rejected");
    }
    this.status = ConsentStatus.REJECTED;
    this.updatedAt = now;
  }

  public boolean allowsAgendaAccess() {
    return status == ConsentStatus.ACCEPTED && authorizationId != null;
  }

  public UUID id() {
    return id;
  }

  public UUID beneficiaryId() {
    return beneficiaryId;
  }

  public String agreementId() {
    return agreementId;
  }

  public String authorizationId() {
    return authorizationId;
  }

  public ConsentStatus status() {
    return status;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
