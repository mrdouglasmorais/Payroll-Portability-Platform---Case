package br.com.payroll.portability.integration.central;

import java.util.List;

public interface CentralGateway {

  String createAuthorization(String beneficiaryExternalRef, String agreementId, String correlationId);

  List<CentralAgendaItem> fetchAgenda(String beneficiaryExternalRef, String correlationId);

  String submitPortability(
      CentralPortabilityRequest request, String idempotencyKey, String correlationId);

  CentralStatus pollStatus(String protocol, String correlationId);
}
