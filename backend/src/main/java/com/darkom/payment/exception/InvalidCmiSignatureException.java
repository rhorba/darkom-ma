package com.darkom.payment.exception;

public class InvalidCmiSignatureException extends RuntimeException {

  public InvalidCmiSignatureException() {
    super("Invalid or missing CMI callback signature");
  }
}
