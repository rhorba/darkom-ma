package com.darkom.lease.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LeaseExceptionHandler {

  @ExceptionHandler(LeaseNotFoundException.class)
  public ProblemDetail handleLeaseNotFound(LeaseNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(UnitAlreadyLeasedException.class)
  public ProblemDetail handleUnitAlreadyLeased(UnitAlreadyLeasedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(TenantNotFoundException.class)
  public ProblemDetail handleTenantNotFound(TenantNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(InvalidLeaseDatesException.class)
  public ProblemDetail handleInvalidLeaseDates(InvalidLeaseDatesException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }
}
