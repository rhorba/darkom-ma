package com.darkom.property.exception;

import java.util.UUID;

public class UnitNotFoundException extends RuntimeException {

  public UnitNotFoundException(UUID id) {
    super("Unit not found: " + id);
  }
}
