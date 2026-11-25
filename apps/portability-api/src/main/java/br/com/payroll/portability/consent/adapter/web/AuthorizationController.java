package br.com.payroll.portability.consent.adapter.web;

import br.com.payroll.portability.consent.application.RequestAuthorizationService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorizationController {

  private final RequestAuthorizationService service;

  public AuthorizationController(RequestAuthorizationService service) {
    this.service = service;
  }

  @PostMapping("/api/v1/autorizacoes")
  public ResponseEntity<Map<String, String>> create(
      @RequestBody Map<String, String> body,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
    String authId =
        service.authorize(
            body.get("beneficiarioId"),
            body.get("convenioId"),
            correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("autorizacaoId", authId));
  }
}
