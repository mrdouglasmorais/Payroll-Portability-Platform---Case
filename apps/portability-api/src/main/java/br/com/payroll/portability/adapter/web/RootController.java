package br.com.payroll.portability.adapter.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

  @GetMapping("/api/v1/status")
  public Map<String, String> status() {
    return Map.of(
        "service", "portability-api",
        "milestone", "M10",
        "status", "UP");
  }
}
