package com.darkom.maintenance.exception;

import java.util.UUID;

/**
 * Also thrown for a request that exists but the caller has no access to - avoids leaking existence,
 * same convention as LeaseNotFoundException/PaymentNotFoundException.
 */
public class MaintenanceRequestNotFoundException extends RuntimeException {

  public MaintenanceRequestNotFoundException(UUID id) {
    super("Maintenance request not found: " + id);
  }
}
