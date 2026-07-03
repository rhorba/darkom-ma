package com.darkom.lease.exception;

public class TenantNotFoundException extends RuntimeException {

  public TenantNotFoundException(String email) {
    super("No tenant account found for email: " + email);
  }
}
