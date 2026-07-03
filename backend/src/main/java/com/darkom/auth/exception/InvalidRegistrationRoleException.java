package com.darkom.auth.exception;

public class InvalidRegistrationRoleException extends RuntimeException {

  public InvalidRegistrationRoleException() {
    super("ADMIN accounts cannot be created via self-registration");
  }
}
