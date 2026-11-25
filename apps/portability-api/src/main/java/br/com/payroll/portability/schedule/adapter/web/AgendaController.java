package br.com.payroll.portability.schedule.adapter.web;

import br.com.payroll.portability.schedule.application.SyncAgendaService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgendaController {

  private final SyncAgendaService service;

  public AgendaController(SyncAgendaService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/beneficiarios/{beneficiarioId}/agenda")
  public List<Map<String, Object>> agenda(
      @PathVariable String beneficiarioId,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
    return service.agenda(
        beneficiarioId,
        correlationId == null || correlationId.isBlank()
            ? UUID.randomUUID().toString()
            : correlationId);
  }
}
