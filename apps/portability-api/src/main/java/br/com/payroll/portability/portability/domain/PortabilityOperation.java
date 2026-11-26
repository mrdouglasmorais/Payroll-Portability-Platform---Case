package br.com.payroll.portability.portability.domain;

import br.com.payroll.portability.shared.domain.DomainException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PortabilityOperation {

  private final UUID id;
  private final UUID beneficiaryId;
  private final String assigneeCreditor;
  private final ReceivingAccount receivingAccount;
  private final Set<String> externalInstallmentIds;
  private final String correlationId;
  private final Instant createdAt;
  private BusinessStatus businessStatus;
  private String rejectionReason;
  private String centralProtocol;
  private Instant updatedAt;
  private final List<OperationEvent> newEvents = new ArrayList<>();
  private int nextSequence = 1;

  private PortabilityOperation(
      UUID id,
      UUID beneficiaryId,
      String assigneeCreditor,
      ReceivingAccount receivingAccount,
      Set<String> externalInstallmentIds,
      String correlationId,
      Instant createdAt,
      BusinessStatus businessStatus,
      String rejectionReason,
      String centralProtocol,
      Instant updatedAt,
      int nextSequence) {
    this.id = id;
    this.beneficiaryId = beneficiaryId;
    this.assigneeCreditor = assigneeCreditor;
    this.receivingAccount = receivingAccount;
    this.externalInstallmentIds = externalInstallmentIds;
    this.correlationId = correlationId;
    this.createdAt = createdAt;
    this.businessStatus = businessStatus;
    this.rejectionReason = rejectionReason;
    this.centralProtocol = centralProtocol;
    this.updatedAt = updatedAt;
    this.nextSequence = nextSequence;
  }

  public static PortabilityOperation reconstitute(
      UUID id,
      UUID beneficiaryId,
      String assigneeCreditor,
      ReceivingAccount receivingAccount,
      Set<String> installmentIds,
      String correlationId,
      Instant createdAt,
      BusinessStatus businessStatus,
      String rejectionReason,
      String centralProtocol,
      Instant updatedAt,
      int nextSequence) {
    return new PortabilityOperation(
        id,
        beneficiaryId,
        assigneeCreditor,
        receivingAccount,
        new LinkedHashSet<>(installmentIds),
        correlationId,
        createdAt,
        businessStatus,
        rejectionReason,
        centralProtocol,
        updatedAt,
        nextSequence);
  }

  public static PortabilityOperation create(
      UUID beneficiaryId,
      String assigneeCreditor,
      ReceivingAccount receivingAccount,
      Collection<String> installmentIds,
      String correlationId,
      Instant now) {
    Objects.requireNonNull(beneficiaryId, "beneficiaryId");
    Objects.requireNonNull(assigneeCreditor, "assigneeCreditor");
    Objects.requireNonNull(receivingAccount, "receivingAccount");
    Objects.requireNonNull(installmentIds, "installmentIds");
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(now, "now");

    if (assigneeCreditor.isBlank()) {
      throw new DomainException("Assignee creditor is required");
    }
    if (installmentIds.isEmpty()) {
      throw new DomainException("At least one installment is required");
    }
    if (installmentIds.stream().anyMatch(id -> id == null || id.isBlank())) {
      throw new DomainException("Installment ids must be non-blank");
    }

    Set<String> ordered = new LinkedHashSet<>(installmentIds.stream().sorted().toList());
    if (ordered.size() != installmentIds.size()) {
      throw new DomainException("Installment ids must be distinct");
    }

    PortabilityOperation operation =
        new PortabilityOperation(
            UUID.randomUUID(),
            beneficiaryId,
            assigneeCreditor,
            receivingAccount,
            ordered,
            correlationId,
            now,
            BusinessStatus.PENDENTE,
            null,
            null,
            now,
            1);
    operation.recordEvent(
        "OPERATION_CREATED",
        "Portability operation created",
        now);
    return operation;
  }

  public void attachProtocol(String protocol, Instant now) {
    requirePending();
    if (protocol == null || protocol.isBlank()) {
      throw new DomainException("Central protocol is required");
    }
    if (this.centralProtocol != null) {
      throw new DomainException("Central protocol already attached");
    }
    this.centralProtocol = protocol;
    this.updatedAt = now;
    recordEvent("PROTOCOL_RECEIVED", "Central protocol attached", now);
  }

  public void confirm(Instant now) {
    transitionTo(BusinessStatus.CONFIRMADA, null, now);
    recordEvent("OPERATION_CONFIRMED", "Operation confirmed by Central", now);
  }

  public void reject(String reason, Instant now) {
    if (reason == null || reason.isBlank()) {
      throw new DomainException("Rejection reason is required");
    }
    transitionTo(BusinessStatus.REJEITADA, reason, now);
    recordEvent("OPERATION_REJECTED", reason, now);
  }

  /** Append-only technical timeline event; does not change business status. */
  public void recordTechnicalEvent(String type, String description, Instant now) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(now, "now");
    recordEvent(type, description, now);
    this.updatedAt = now;
  }

  private void transitionTo(BusinessStatus next, String reason, Instant now) {
    if (!businessStatus.canTransitionTo(next)) {
      throw new DomainException(
          "Cannot transition from " + businessStatus + " to " + next);
    }
    this.businessStatus = next;
    this.rejectionReason = reason;
    this.updatedAt = now;
  }

  private void requirePending() {
    if (businessStatus != BusinessStatus.PENDENTE) {
      throw new DomainException("Operation is not pending: " + businessStatus);
    }
  }

  private void recordEvent(String type, String description, Instant now) {
    newEvents.add(
        new OperationEvent(
            UUID.randomUUID(),
            id,
            nextSequence++,
            type,
            description,
            correlationId,
            now));
  }

  public UUID id() {
    return id;
  }

  public UUID beneficiaryId() {
    return beneficiaryId;
  }

  public String assigneeCreditor() {
    return assigneeCreditor;
  }

  public ReceivingAccount receivingAccount() {
    return receivingAccount;
  }

  public Set<String> externalInstallmentIds() {
    return Collections.unmodifiableSet(externalInstallmentIds);
  }

  public String correlationId() {
    return correlationId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public BusinessStatus businessStatus() {
    return businessStatus;
  }

  public String rejectionReason() {
    return rejectionReason;
  }

  public String centralProtocol() {
    return centralProtocol;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public List<OperationEvent> pullNewEvents() {
    List<OperationEvent> copy = List.copyOf(newEvents);
    newEvents.clear();
    return copy;
  }
}
