package br.com.payroll.portability.consent.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorizationRequest(
    @NotBlank @Size(max = 64) String beneficiarioId,
    @NotBlank @Size(max = 64) String convenioId) {}
