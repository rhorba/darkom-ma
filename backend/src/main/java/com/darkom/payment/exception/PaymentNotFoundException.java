package com.darkom.payment.exception;

import java.util.UUID;

/** Also thrown for a payment/lease the caller has no access to - avoids leaking existence. */
public class PaymentNotFoundException extends RuntimeException {

  public PaymentNotFoundException(UUID id) {
    super("Payment not found: " + id);
  }

  public PaymentNotFoundException(String message) {
    super(message);
  }
}
