package br.com.payroll.portability.portability.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReceivingAccountRequest(
    @NotBlank @Size(max = 8) String banco,
    @NotBlank @Size(max = 16) String agencia,
    @NotBlank @Size(max = 32) String conta) {}
