package br.com.payroll.central.adapter.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

  @GetMapping("/status")
  public Map<String, String> status() {
    return Map.of(
        "service", "central-simulator",
        "milestone", "M1",
        "status", "UP");
  }
}
