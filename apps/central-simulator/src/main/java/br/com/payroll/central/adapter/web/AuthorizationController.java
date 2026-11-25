package br.com.payroll.central.adapter.web;

import br.com.payroll.central.application.CentralProtocolService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthorizationController {

  private final CentralProtocolService service;

  public AuthorizationController(CentralProtocolService service) {
    this.service = service;
  }

  @PostMapping("/autorizacoes")
  public ResponseEntity<Map<String, String>> create(
      @RequestBody Map<String, String> body, HttpServletRequest request) {
    rejectIfSimulatedOutage(request);
    String beneficiaryId = required(body, "beneficiarioId");
    String agreementId = required(body, "convenioId");
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createAuthorization(beneficiaryId, agreementId));
  }

  private static String required(Map<String, String> body, String field) {
    String value = body.get(field);
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " required");
    }
    return value;
  }

  private static void rejectIfSimulatedOutage(HttpServletRequest request) {
    String fault = request.getHeader("X-Sim-Fault");
    if (fault != null && "UNAVAILABLE".equalsIgnoreCase(fault.trim())) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated outage");
    }
  }
}
