package com.darkom.payment.exception;

/**
 * A caller who can legitimately read a lease (owning Landlord/PM, Admin) is not automatically
 * allowed to pay its rent - only the lease's own Tenant can. Deliberately not a
 * PaymentNotFoundException: this caller genuinely can see the lease exists.
 */
public class PaymentNotAuthorizedException extends RuntimeException {

  public PaymentNotAuthorizedException() {
    super("Only the tenant on this lease can initiate a payment for it");
  }
}
