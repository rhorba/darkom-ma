package com.darkom.lease.exception;

public class InvalidLeaseDatesException extends RuntimeException {

  public InvalidLeaseDatesException() {
    super("Lease end date must be after the start date");
  }
}
