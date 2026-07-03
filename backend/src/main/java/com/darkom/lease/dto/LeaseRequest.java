package com.darkom.lease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeaseRequest(
    @NotNull UUID unitId,
    @NotBlank @Email String tenantEmail,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull @DecimalMin(value = "0.01", message = "Monthly rent must be positive")
        BigDecimal monthlyRent) {}
