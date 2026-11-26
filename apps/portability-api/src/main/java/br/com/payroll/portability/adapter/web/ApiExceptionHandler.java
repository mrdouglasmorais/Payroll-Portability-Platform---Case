package br.com.payroll.portability.adapter.web;

import br.com.payroll.portability.integration.central.CentralException;
import br.com.payroll.portability.portability.application.IdempotencyConflictException;
import br.com.payroll.portability.shared.domain.DomainException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<Map<String, String>> domain(DomainException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(IdempotencyConflictException.class)
  public ResponseEntity<Map<String, String>> idempotency(IdempotencyConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(CentralException.class)
  public ResponseEntity<Map<String, String>> central(CentralException ex) {
    HttpStatus status =
        ex.isTransientFailure() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
    if (ex.statusCode() == 403) {
      status = HttpStatus.FORBIDDEN;
    } else if (ex.statusCode() == 409) {
      status = HttpStatus.CONFLICT;
    }
    return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
  }

  /**
   * Competing portability creates (two tabs / two keys, same parcel) must surface as 409, never
   * as an unhandled 500.
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, String>> conflictState(IllegalStateException ex) {
    if (isInstallmentConflict(ex.getMessage())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(
              Map.of(
                  "error",
                  ex.getMessage() == null ? "Parcela indisponível" : ex.getMessage()));
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", ex.getMessage() == null ? "erro" : ex.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> dataIntegrity(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause().getMessage();
    if (message != null
        && (message.contains("uq_active_installment_reservation")
            || message.contains("installment_reservations"))) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "Parcela já reservada"));
    }
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", "Conflito de integridade nos dados da operação"));
  }

  private static boolean isInstallmentConflict(String message) {
    if (message == null) {
      return false;
    }
    return message.startsWith("Parcela já reservada")
        || message.startsWith("Parcela já portada")
        || message.startsWith("Parcela indisponível");
  }
}
