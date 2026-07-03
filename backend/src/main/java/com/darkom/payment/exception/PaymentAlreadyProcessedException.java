package com.darkom.payment.exception;

/** A callback for a transaction that has already been settled (PAID/FAILED) - replay guard. */
public class PaymentAlreadyProcessedException extends RuntimeException {

  public PaymentAlreadyProcessedException() {
    super("Payment has already been processed");
  }
}
