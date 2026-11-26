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
import br.com.payroll.portability.shared.observability.LogEvents;
import br.com.payroll.portability.shared.observability.LogFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SyncAgendaService {

  private static final Logger log = LoggerFactory.getLogger(SyncAgendaService.class);

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
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Beneficiário não encontrado"));

    if (!consentRepository.hasAcceptedConsent(beneficiaryId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não há consentimento aceito");
    }

    List<CentralAgendaItem> remote;
    try {
      remote = centralGateway.fetchAgenda(beneficiaryExternalRef, correlationId);
    } catch (CentralException ex) {
      if (ex.statusCode() == 403) {
        // correlationId is already in MDC — do not addKeyValue it (ECS duplicate key).
        log.atWarn()
            .addKeyValue(LogFields.EVENT, LogEvents.CENTRAL_AGENDA_FORBIDDEN)
            .addKeyValue(LogFields.ERROR_CODE, ex.failureCode().name())
            .log("Central forbade agenda (missing authorization?)");
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Agenda da Central proibida");
      }
      if (ex.isTransientFailure()) {
        throw ex;
      }
      throw new DomainException("Falha na agenda da Central: " + ex.getMessage());
    }

    List<CentralAgendaItem> authorized =
        remote.stream()
            .filter(
                item ->
                    consentRepository
                        .findAccepted(beneficiaryId, item.convenioId())
                        .isPresent())
            .toList();

    tx.executeWithoutResult(status -> persistSnapshots(beneficiaryId, authorized));
    return toResponse(authorized);
  }

  private void persistSnapshots(UUID beneficiaryId, List<CentralAgendaItem> remote) {
    for (CentralAgendaItem item : remote) {
      var existing = installmentRepository.findByExternalId(item.parcelaId());
      if (existing.isPresent()) {
        InstallmentSnapshot current = existing.get();
        if (current.status() == InstallmentStatus.OPEN) {
          // Conditional update: never overwrite status or effective_* if portability won the race.
          installmentRepository.updateOpenAgendaAttributes(
              current.withOpenRefresh(
                  item.convenioId(), Money.of(item.valor()), item.dataDesconto()));
        }
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
