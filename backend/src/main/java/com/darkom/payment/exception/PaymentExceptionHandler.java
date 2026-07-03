package com.darkom.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

  @ExceptionHandler(PaymentNotFoundException.class)
  public ProblemDetail handlePaymentNotFound(PaymentNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(NoPendingPaymentException.class)
  public ProblemDetail handleNoPendingPayment(NoPendingPaymentException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(InvalidCmiSignatureException.class)
  public ProblemDetail handleInvalidSignature(InvalidCmiSignatureException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(CallbackAmountMismatchException.class)
  public ProblemDetail handleAmountMismatch(CallbackAmountMismatchException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(PaymentAlreadyProcessedException.class)
  public ProblemDetail handleAlreadyProcessed(PaymentAlreadyProcessedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(PaymentNotAuthorizedException.class)
  public ProblemDetail handleNotAuthorized(PaymentNotAuthorizedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }
}
