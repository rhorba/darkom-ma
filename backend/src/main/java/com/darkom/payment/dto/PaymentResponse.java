package com.darkom.payment.dto;

import com.darkom.payment.entity.Payment;
import com.darkom.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID leaseId,
    BigDecimal amount,
    LocalDate dueDate,
    Instant paidAt,
    PaymentStatus status,
    String cmiTransactionId) {

  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getLeaseId(),
        payment.getAmount(),
        payment.getDueDate(),
        payment.getPaidAt(),
        payment.getStatus(),
        payment.getCmiTransactionId());
  }
}
