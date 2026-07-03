package com.darkom.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {

  public InvalidRefreshTokenException() {
    super("Refresh token is missing, expired, or already used");
  }
}
