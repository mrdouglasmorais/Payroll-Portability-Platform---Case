package br.com.payroll.portability.adapter.web;

import br.com.payroll.portability.integration.central.CentralException;
import br.com.payroll.portability.portability.application.IdempotencyConflictException;
import br.com.payroll.portability.shared.domain.DomainException;
import br.com.payroll.portability.shared.domain.InstallmentConflictException;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ProblemDetail domain(DomainException ex) {
    return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
  }

  @ExceptionHandler(InstallmentConflictException.class)
  public ProblemDetail installmentConflict(InstallmentConflictException ex) {
    ProblemDetail detail =
        problem(HttpStatus.CONFLICT, "Conflito de parcela", ex.getMessage());
    detail.setProperty("reason", ex.reason().name());
    detail.setProperty("parcelaId", ex.externalInstallmentId());
    return detail;
  }

  @ExceptionHandler(IdempotencyConflictException.class)
  public ProblemDetail idempotency(IdempotencyConflictException ex) {
    return problem(HttpStatus.CONFLICT, "Conflito de idempotência", ex.getMessage());
  }

  @ExceptionHandler(CentralException.class)
  public ProblemDetail central(CentralException ex) {
    HttpStatus status =
        ex.isTransientFailure() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
    if (ex.statusCode() == 403) {
      status = HttpStatus.FORBIDDEN;
    } else if (ex.statusCode() == 409) {
      status = HttpStatus.CONFLICT;
    }
    return problem(status, "Falha na integração com a Central", ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail dataIntegrity(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause().getMessage();
    if (message != null
        && (message.contains("uq_active_installment_reservation")
            || message.contains("installment_reservations"))) {
      return problem(HttpStatus.CONFLICT, "Conflito de parcela", "Parcela já reservada");
    }
    return problem(
        HttpStatus.CONFLICT,
        "Conflito de integridade",
        "Conflito de integridade nos dados da operação");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail beanValidation(MethodArgumentNotValidException ex) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return problem(
        HttpStatus.BAD_REQUEST,
        "Validação falhou",
        detail.isBlank() ? "Payload inválido" : detail);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ProblemDetail methodValidation(HandlerMethodValidationException ex) {
    return problem(HttpStatus.BAD_REQUEST, "Validação falhou", ex.getMessage());
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ProblemDetail missingHeader(MissingRequestHeaderException ex) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "Cabeçalho obrigatório ausente",
        ex.getHeaderName() + " é obrigatório");
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> responseStatus(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    ProblemDetail detail =
        problem(
            status,
            status.getReasonPhrase(),
            ex.getReason() == null ? status.getReasonPhrase() : ex.getReason());
    return ResponseEntity.status(status).body(detail);
  }

  private static ProblemDetail problem(HttpStatus status, String title, String detail) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setTitle(title);
    pd.setType(URI.create("about:blank"));
    // Backward-compatible alias for the React client until it reads `detail` only.
    pd.setProperty("error", detail);
    return pd;
  }
}
