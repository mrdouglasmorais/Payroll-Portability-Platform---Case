package br.com.payroll.portability.integration.central;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CentralAgendaItem(
    String parcelaId,
    String convenioId,
    BigDecimal valor,
    LocalDate dataDesconto,
    String status) {}
