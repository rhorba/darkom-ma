package com.darkom.lease.exception;

import java.util.UUID;

public class UnitAlreadyLeasedException extends RuntimeException {

  public UnitAlreadyLeasedException(UUID unitId) {
    super("Unit already has an active lease: " + unitId);
  }
}
