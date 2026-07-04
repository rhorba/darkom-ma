package com.darkom.maintenance.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MaintenanceExceptionHandler {

  @ExceptionHandler(MaintenanceRequestNotFoundException.class)
  public ProblemDetail handleNotFound(MaintenanceRequestNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(MaintenanceNotAuthorizedException.class)
  public ProblemDetail handleNotAuthorized(MaintenanceNotAuthorizedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(InvalidPhotoException.class)
  public ProblemDetail handleInvalidPhoto(InvalidPhotoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }
}
