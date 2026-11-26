package br.com.payroll.portability.consent.adapter.web;

import br.com.payroll.portability.consent.adapter.web.dto.AuthorizationRequest;
import br.com.payroll.portability.consent.adapter.web.dto.AuthorizationResponse;
import br.com.payroll.portability.consent.application.RequestAuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class AuthorizationController {

  private final RequestAuthorizationService service;

  public AuthorizationController(RequestAuthorizationService service) {
    this.service = service;
  }

  @PostMapping("/api/v1/autorizacoes")
  public ResponseEntity<AuthorizationResponse> create(
      @Valid @RequestBody AuthorizationRequest body,
      @RequestHeader(value = "X-Correlation-Id", required = false) @Size(max = 128)
          String correlationId) {
    String authId =
        service.authorize(
            body.beneficiarioId(),
            body.convenioId(),
            correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId);
    return ResponseEntity.status(HttpStatus.CREATED).body(new AuthorizationResponse(authId));
  }
}
