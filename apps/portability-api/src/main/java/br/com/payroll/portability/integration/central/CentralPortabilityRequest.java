package br.com.payroll.portability.integration.central;

import java.util.List;

public record CentralPortabilityRequest(
    String beneficiarioId,
    List<String> parcelas,
    String credorCessionario,
    String banco,
    String agencia,
    String conta) {}
