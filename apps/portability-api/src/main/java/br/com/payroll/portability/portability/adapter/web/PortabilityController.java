package br.com.payroll.portability.portability.adapter.web;

import br.com.payroll.portability.beneficiary.application.BeneficiaryRepository;
import br.com.payroll.portability.portability.application.CreatePortabilityService;
import br.com.payroll.portability.portability.application.PortabilityOperationRepository;
import br.com.payroll.portability.portability.application.ProcessingJob;
import br.com.payroll.portability.portability.application.ProcessingJobRepository;
import br.com.payroll.portability.portability.application.RequestHash;
import br.com.payroll.portability.portability.domain.PortabilityCommand;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import br.com.payroll.portability.portability.domain.TechnicalJobStatus;
import br.com.payroll.portability.shared.domain.DomainException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PortabilityController {

  private final CreatePortabilityService createService;
  private final PortabilityOperationRepository operationRepository;
  private final ProcessingJobRepository jobRepository;
  private final BeneficiaryRepository beneficiaryRepository;

  public PortabilityController(
      CreatePortabilityService createService,
      PortabilityOperationRepository operationRepository,
      ProcessingJobRepository jobRepository,
      BeneficiaryRepository beneficiaryRepository) {
    this.createService = createService;
    this.operationRepository = operationRepository;
    this.jobRepository = jobRepository;
    this.beneficiaryRepository = beneficiaryRepository;
  }

  @PostMapping("/api/v1/operacoes-portabilidade")
  public ResponseEntity<Map<String, String>> create(
      @RequestBody Map<String, Object> body,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new DomainException("Idempotency-Key is required");
    }
    String beneficiaryExternalRef = string(body, "beneficiarioId");
    UUID beneficiaryId =
        beneficiaryRepository
            .findIdByExternalRef(beneficiaryExternalRef)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown beneficiary"));

    @SuppressWarnings("unchecked")
    List<String> parcelas =
        body.get("parcelas") instanceof List<?> list
            ? list.stream().map(Object::toString).toList()
            : List.of();
    if (parcelas.isEmpty()) {
      throw new DomainException("parcelas required");
    }
    Set<String> parcelaSet = new LinkedHashSet<>(parcelas);

    String creditor = string(body, "credorCessionario");
    @SuppressWarnings("unchecked")
    Map<String, Object> accountMap =
        body.get("contaRecebimento") instanceof Map<?, ?> map
            ? (Map<String, Object>) map
            : Map.of();
    ReceivingAccount account =
        new ReceivingAccount(
            string(accountMap, "banco"),
            string(accountMap, "agencia"),
            string(accountMap, "conta"));

    String corr =
        correlationId == null || correlationId.isBlank()
            ? UUID.randomUUID().toString()
            : correlationId;
    String hash = RequestHash.forPortability(beneficiaryExternalRef, parcelaSet, creditor, account);

    UUID operationId =
        createService.create(
            new PortabilityCommand(
                beneficiaryId, parcelaSet, creditor, account, idempotencyKey, corr, hash));

    return ResponseEntity.accepted().body(Map.of("operationId", operationId.toString()));
  }

  @GetMapping("/api/v1/operacoes-portabilidade/{operationId}")
  public Map<String, Object> status(@PathVariable UUID operationId) {
    PortabilityOperation operation =
        operationRepository
            .findById(operationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("operationId", operation.id().toString());
    body.put("status", operation.businessStatus().name());
    body.put("motivoRejeicao", operation.rejectionReason());
    body.put("correlationId", operation.correlationId());
    body.put("processing", processingView(operationId));
    return body;
  }

  @GetMapping("/api/v1/operacoes-portabilidade/{operationId}/eventos")
  public List<Map<String, Object>> events(@PathVariable UUID operationId) {
    if (operationRepository.findById(operationId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    }
    List<Map<String, Object>> events = new ArrayList<>();
    operationRepository
        .listEvents(operationId)
        .forEach(
            event -> {
              Map<String, Object> row = new LinkedHashMap<>();
              row.put("eventType", event.eventType());
              row.put("sequence", event.sequenceNo());
              row.put("occurredAt", event.occurredAt().toString());
              row.put("description", event.description());
              events.add(row);
            });
    return events;
  }

  private Map<String, Object> processingView(UUID operationId) {
    Map<String, Object> processing = new LinkedHashMap<>();
    ProcessingJob job = jobRepository.findByOperationId(operationId).orElse(null);
    if (job == null) {
      processing.put("status", "UNKNOWN");
      processing.put("attemptCount", 0);
      processing.put("errorCode", null);
      processing.put("requiresAction", false);
      return processing;
    }
    TechnicalJobStatus tech = job.technicalStatus();
    String status;
    boolean requiresAction = false;
    if (tech == TechnicalJobStatus.EXHAUSTED) {
      status = "REQUIRES_REVIEW";
      requiresAction = true;
    } else if (tech == TechnicalJobStatus.SUCCEEDED) {
      status = "SUCCEEDED";
    } else if (tech == TechnicalJobStatus.RUNNING) {
      status = "RUNNING";
    } else if (tech == TechnicalJobStatus.FAILED) {
      status = "RETRYING";
    } else {
      status = "PENDING";
    }
    processing.put("status", status);
    processing.put("attemptCount", job.attemptCount());
    processing.put("errorCode", classifyError(job.lastError()));
    processing.put("requiresAction", requiresAction);
    return processing;
  }

  private static String classifyError(String lastError) {
    if (lastError == null || lastError.isBlank()) {
      return null;
    }
    String upper = lastError.toUpperCase();
    if (upper.contains("UNAVAILABLE") || upper.contains("503") || upper.contains("502")) {
      return "CENTRAL_UNAVAILABLE";
    }
    if (upper.contains("POLL_DEADLINE")) {
      return "POLL_DEADLINE_EXCEEDED";
    }
    if (upper.contains("RATE LIMITED") || upper.contains("429")) {
      return "CENTRAL_RATE_LIMITED";
    }
    if (upper.contains("SIGNATURE")) {
      return "HMAC_FAILURE";
    }
    return "INTEGRATION_ERROR";
  }

  private static String string(Map<String, ?> body, String field) {
    Object value = body.get(field);
    if (value == null || value.toString().isBlank()) {
      throw new DomainException(field + " is required");
    }
    return value.toString();
  }
}
