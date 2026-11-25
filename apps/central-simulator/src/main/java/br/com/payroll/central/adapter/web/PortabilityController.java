package br.com.payroll.central.adapter.web;

import br.com.payroll.central.application.CentralProtocolService;
import br.com.payroll.central.application.CentralProtocolService.ProtocolStatusView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PortabilityController {

  private final CentralProtocolService service;

  public PortabilityController(CentralProtocolService service) {
    this.service = service;
  }

  @PostMapping("/operacoes-portabilidade")
  public ResponseEntity<Map<String, String>> create(HttpServletRequest request) throws Exception {
    String idempotencyKey = request.getHeader("Idempotency-Key");
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key required");
    }
    byte[] bodyBytes =
        request instanceof CachedBodyHttpServletRequest cached
            ? cached.getCachedBody()
            : request.getInputStream().readAllBytes();
    String rawBody = new String(bodyBytes);
    String fault = request.getHeader("X-Sim-Fault");
    return ResponseEntity.accepted()
        .body(service.createPortability(rawBody, idempotencyKey, fault));
  }

  @GetMapping("/operacoes-portabilidade/{protocolo}")
  public ResponseEntity<Map<String, Object>> status(@PathVariable String protocolo) {
    ProtocolStatusView view = service.status(protocolo);
    if (view.pendingEmpty()) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT)
          .header("Retry-After", "1")
          .build();
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", view.status());
    body.put("motivoRejeicao", view.motivoRejeicao());
    return ResponseEntity.ok(body);
  }
}
