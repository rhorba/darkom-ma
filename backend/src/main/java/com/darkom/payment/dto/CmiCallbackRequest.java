package com.darkom.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Shape CMI's server-to-server callback would POST. Real CMI gateways typically use form-urlencoded
 * fields with gateway-specific names - this is a placeholder shape until real integration docs
 * exist (see CmiClient's class-level note); only the field set and signature scheme would need to
 * change, not the verification logic itself.
 */
public record CmiCallbackRequest(
    @NotBlank String cmiTransactionId,
    @NotNull CmiCallbackStatus status,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank String signature) {}
