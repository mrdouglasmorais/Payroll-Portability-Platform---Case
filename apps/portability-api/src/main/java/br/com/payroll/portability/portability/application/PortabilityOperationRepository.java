package br.com.payroll.portability.portability.application;

import br.com.payroll.portability.portability.domain.PortabilityOperation;
import java.util.Optional;
import java.util.UUID;

public interface PortabilityOperationRepository {

  void insertAtomicCreate(
      PortabilityOperation operation,
      String idempotencyScope,
      String idempotencyKey,
      String requestHash);

  Optional<UUID> findOperationIdByIdempotency(String scope, String key);

  Optional<String> findRequestHashByIdempotency(String scope, String key);

  Optional<PortabilityOperation> findById(UUID operationId);
}
