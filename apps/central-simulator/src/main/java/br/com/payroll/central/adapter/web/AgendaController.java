package br.com.payroll.central.adapter.web;

import br.com.payroll.central.application.CentralProtocolService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgendaController {

  private final CentralProtocolService service;

  public AgendaController(CentralProtocolService service) {
    this.service = service;
  }

  @GetMapping("/beneficiarios/{beneficiarioId}/agenda")
  public List<Map<String, Object>> agenda(@PathVariable String beneficiarioId) {
    return service.agenda(beneficiarioId);
  }
}
