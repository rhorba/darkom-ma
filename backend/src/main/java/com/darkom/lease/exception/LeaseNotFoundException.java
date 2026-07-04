package com.darkom.lease.exception;

import java.util.UUID;

/**
 * Also thrown for a lease that exists but the caller has no access to - avoids leaking existence.
 */
public class LeaseNotFoundException extends RuntimeException {

  public LeaseNotFoundException(UUID id) {
    super("Lease not found: " + id);
  }

  public LeaseNotFoundException(String message) {
    super(message);
  }
}
