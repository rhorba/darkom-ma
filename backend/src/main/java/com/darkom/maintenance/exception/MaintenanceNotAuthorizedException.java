package com.darkom.maintenance.exception;

/** Thrown when a Tenant tries to submit a request for a unit they have no active lease on. */
public class MaintenanceNotAuthorizedException extends RuntimeException {

  public MaintenanceNotAuthorizedException() {
    super("You do not have an active lease on this unit");
  }
}
