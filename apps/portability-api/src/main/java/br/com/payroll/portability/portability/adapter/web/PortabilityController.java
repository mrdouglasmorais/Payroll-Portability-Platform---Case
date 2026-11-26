package br.com.payroll.portability.portability.adapter.web;

import br.com.payroll.portability.beneficiary.application.BeneficiaryRepository;
import br.com.payroll.portability.portability.adapter.web.dto.CreatePortabilityRequest;
import br.com.payroll.portability.portability.adapter.web.dto.CreatePortabilityResponse;
import br.com.payroll.portability.portability.adapter.web.dto.OperationEventResponse;
import br.com.payroll.portability.portability.adapter.web.dto.OperationListItemResponse;
import br.com.payroll.portability.portability.adapter.web.dto.OperationPageResponse;
import br.com.payroll.portability.portability.adapter.web.dto.OperationStatusResponse;
import br.com.payroll.portability.portability.adapter.web.dto.ProcessingStatusResponse;
import br.com.payroll.portability.portability.adapter.web.dto.ReceivingAccountView;
import br.com.payroll.portability.portability.application.CreatePortabilityService;
import br.com.payroll.portability.portability.application.OperationPage;
import br.com.payroll.portability.portability.application.OperationSummary;
import br.com.payroll.portability.portability.application.PortabilityOperationRepository;
import br.com.payroll.portability.portability.application.ProcessingJob;
import br.com.payroll.portability.portability.application.ProcessingJobRepository;
import br.com.payroll.portability.portability.application.RequestHash;
import br.com.payroll.portability.portability.domain.BusinessStatus;
import br.com.payroll.portability.portability.domain.PortabilityCommand;
import br.com.payroll.portability.portability.domain.PortabilityOperation;
import br.com.payroll.portability.portability.domain.ReceivingAccount;
import br.com.payroll.portability.portability.domain.TechnicalJobStatus;
import br.com.payroll.portability.shared.domain.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Validated
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

  @GetMapping("/api/v1/operacoes-portabilidade")
  public OperationPageResponse list(
      @RequestParam("beneficiarioId") @NotBlank @Size(max = 128) String beneficiarioId,
      @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
      @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) int size) {
    OperationPage result =
        operationRepository.pageByBeneficiaryExternalRef(beneficiarioId.trim(), page, size);
    return new OperationPageResponse(
        result.items().stream().map(this::toListItem).toList(),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages(),
        result.hasNext(),
        result.hasPrevious());
  }

  @PostMapping("/api/v1/operacoes-portabilidade")
  public ResponseEntity<CreatePortabilityResponse> create(
      @Valid @RequestBody CreatePortabilityRequest body,
      @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String idempotencyKey,
      @RequestHeader(value = "X-Correlation-Id", required = false) @Size(max = 128)
          String correlationId) {
    Set<String> parcelaSet = new LinkedHashSet<>(body.parcelas());
    if (parcelaSet.size() != body.parcelas().size()) {
      throw new DomainException("Parcelas duplicadas na requisição");
    }

    UUID beneficiaryId =
        beneficiaryRepository
            .findIdByExternalRef(body.beneficiarioId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Beneficiário desconhecido"));

    ReceivingAccount account =
        new ReceivingAccount(
            body.contaRecebimento().banco(),
            body.contaRecebimento().agencia(),
            body.contaRecebimento().conta());

    String corr =
        correlationId == null || correlationId.isBlank()
            ? UUID.randomUUID().toString()
            : correlationId;
    String hash =
        RequestHash.forPortability(
            body.beneficiarioId(), parcelaSet, body.credorCessionario(), account);

    UUID operationId =
        createService.create(
            new PortabilityCommand(
                beneficiaryId,
                parcelaSet,
                body.credorCessionario(),
                account,
                idempotencyKey,
                corr,
                hash));

    return ResponseEntity.accepted()
        .body(new CreatePortabilityResponse(operationId.toString()));
  }

  @GetMapping("/api/v1/operacoes-portabilidade/{operationId}")
  public OperationStatusResponse status(@PathVariable UUID operationId) {
    PortabilityOperation operation =
        operationRepository
            .findById(operationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontrado"));
    boolean efetivada = operation.businessStatus() == BusinessStatus.CONFIRMADA;
    return new OperationStatusResponse(
        operation.id().toString(),
        operation.businessStatus().name(),
        operation.rejectionReason(),
        operation.correlationId(),
        operation.centralProtocol(),
        ReceivingAccountView.masked(operation.receivingAccount()),
        efetivada,
        processingView(operationId));
  }

  @GetMapping("/api/v1/operacoes-portabilidade/{operationId}/eventos")
  public List<OperationEventResponse> events(@PathVariable UUID operationId) {
    if (operationRepository.findById(operationId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontrado");
    }
    return operationRepository.listEvents(operationId).stream()
        .map(
            event ->
                new OperationEventResponse(
                    event.eventType(),
                    event.sequenceNo(),
                    event.occurredAt().toString(),
                    event.description()))
        .toList();
  }

  private OperationListItemResponse toListItem(OperationSummary summary) {
    boolean efetivada = summary.businessStatus() == BusinessStatus.CONFIRMADA;
    return new OperationListItemResponse(
        summary.id().toString(),
        summary.businessStatus().name(),
        summary.createdAt().toString(),
        summary.correlationId(),
        summary.centralProtocol(),
        ReceivingAccountView.masked(summary.receivingAccount()),
        efetivada);
  }

  private ProcessingStatusResponse processingView(UUID operationId) {
    ProcessingJob job = jobRepository.findByOperationId(operationId).orElse(null);
    if (job == null) {
      return new ProcessingStatusResponse("UNKNOWN", 0, null, false);
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
    } else if (tech == TechnicalJobStatus.FAILED
        || (tech == TechnicalJobStatus.PENDING
            && job.attemptCount() > 0
            && job.lastError() != null)) {
      status = "RETRYING";
    } else {
      status = "PENDING";
    }
    return new ProcessingStatusResponse(
        status, job.attemptCount(), job.lastErrorCode(), requiresAction);
  }
}
