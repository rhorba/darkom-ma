package com.darkom.payment.exception;

/**
 * A validly-signed callback whose claimed amount doesn't match the payment we initiated - defense
 * in depth against a compromised signature source or a CMI-side bug, not just outright forgery.
 */
public class CallbackAmountMismatchException extends RuntimeException {

  public CallbackAmountMismatchException() {
    super("Callback amount does not match the expected payment amount");
  }
}
