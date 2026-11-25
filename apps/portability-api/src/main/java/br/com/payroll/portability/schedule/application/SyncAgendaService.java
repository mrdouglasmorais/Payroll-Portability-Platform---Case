package br.com.payroll.portability.schedule.application;

import br.com.payroll.portability.beneficiary.application.BeneficiaryRepository;
import br.com.payroll.portability.consent.application.ConsentRepository;
import br.com.payroll.portability.integration.central.CentralAgendaItem;
import br.com.payroll.portability.integration.central.CentralException;
import br.com.payroll.portability.integration.central.CentralGateway;
import br.com.payroll.portability.schedule.domain.InstallmentSnapshot;
import br.com.payroll.portability.schedule.domain.InstallmentStatus;
import br.com.payroll.portability.shared.domain.DomainException;
import br.com.payroll.portability.shared.domain.Money;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SyncAgendaService {

  private final BeneficiaryRepository beneficiaryRepository;
  private final ConsentRepository consentRepository;
  private final InstallmentRepository installmentRepository;
  private final CentralGateway centralGateway;
  private final TransactionTemplate tx;

  public SyncAgendaService(
      BeneficiaryRepository beneficiaryRepository,
      ConsentRepository consentRepository,
      InstallmentRepository installmentRepository,
      CentralGateway centralGateway,
      TransactionTemplate tx) {
    this.beneficiaryRepository = beneficiaryRepository;
    this.consentRepository = consentRepository;
    this.installmentRepository = installmentRepository;
    this.centralGateway = centralGateway;
    this.tx = tx;
  }

  public List<Map<String, Object>> agenda(String beneficiaryExternalRef, String correlationId) {
    UUID beneficiaryId =
        beneficiaryRepository
            .findIdByExternalRef(beneficiaryExternalRef)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));

    if (!consentRepository.hasAcceptedConsent(beneficiaryId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No accepted consent");
    }

    List<CentralAgendaItem> remote;
    try {
      remote = centralGateway.fetchAgenda(beneficiaryExternalRef, correlationId);
    } catch (CentralException ex) {
      if (ex.statusCode() == 403) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Central agenda forbidden");
      }
      if (ex.isTransientFailure()) {
        throw ex;
      }
      throw new DomainException("Central agenda failed: " + ex.getMessage());
    }

    tx.executeWithoutResult(status -> persistSnapshots(beneficiaryId, remote));
    return toResponse(remote);
  }

  private void persistSnapshots(UUID beneficiaryId, List<CentralAgendaItem> remote) {
    for (CentralAgendaItem item : remote) {
      var existing = installmentRepository.findByExternalId(item.parcelaId());
      if (existing.isPresent()) {
        continue;
      }
      installmentRepository.save(
          new InstallmentSnapshot(
              UUID.randomUUID(),
              item.parcelaId(),
              beneficiaryId,
              item.convenioId(),
              Money.of(item.valor()),
              item.dataDesconto(),
              InstallmentStatus.OPEN));
    }
  }

  private static List<Map<String, Object>> toResponse(List<CentralAgendaItem> remote) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (CentralAgendaItem item : remote) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("parcelaId", item.parcelaId());
      row.put("convenioId", item.convenioId());
      row.put("valor", item.valor());
      row.put("dataDesconto", item.dataDesconto().toString());
      row.put("status", item.status());
      out.add(row);
    }
    return out;
  }
}
