package com.darkom.property.exception;

import java.util.UUID;

/**
 * Also thrown for a property that exists but the caller has no access to - avoids leaking
 * existence.
 */
public class PropertyNotFoundException extends RuntimeException {

  public PropertyNotFoundException(UUID id) {
    super("Property not found: " + id);
  }
}
