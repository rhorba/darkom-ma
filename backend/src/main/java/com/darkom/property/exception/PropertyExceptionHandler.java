package com.darkom.property.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PropertyExceptionHandler {

  @ExceptionHandler(PropertyNotFoundException.class)
  public ProblemDetail handlePropertyNotFound(PropertyNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(UnitNotFoundException.class)
  public ProblemDetail handleUnitNotFound(UnitNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ManagerNotFoundException.class)
  public ProblemDetail handleManagerNotFound(ManagerNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }
}
