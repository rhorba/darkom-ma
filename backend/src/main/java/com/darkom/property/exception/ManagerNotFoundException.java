package com.darkom.property.exception;

/** Thrown when the given email doesn't belong to an existing PROPERTY_MANAGER user. */
public class ManagerNotFoundException extends RuntimeException {

  public ManagerNotFoundException(String email) {
    super("No property manager account found for: " + email);
  }
}
