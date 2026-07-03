package com.darkom.property.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UnitRequest(
    @NotBlank String label,
    @NotNull @DecimalMin(value = "0.01", message = "Monthly rent must be positive")
        BigDecimal monthlyRent) {}
