package br.com.payroll.portability.portability.adapter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePortabilityRequest(
    @NotBlank @Size(max = 64) String beneficiarioId,
    @NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 64) String> parcelas,
    @NotBlank @Size(max = 128) String credorCessionario,
    @NotNull @Valid ReceivingAccountRequest contaRecebimento) {}
