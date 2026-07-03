package com.darkom.payment.exception;

import java.util.UUID;

public class NoPendingPaymentException extends RuntimeException {

  public NoPendingPaymentException(UUID leaseId) {
    super("No pending payment for lease: " + leaseId);
  }
}
