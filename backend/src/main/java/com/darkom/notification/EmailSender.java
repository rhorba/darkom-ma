package com.darkom.notification;

/**
 * Boundary around the transactional email provider (architecture-darkom.md's system diagram names
 * one but doesn't pick a vendor). No real provider account exists yet (EMAIL_API_KEY is still a
 * placeholder), so {@link MockEmailSender} is the only implementation - same pattern as {@code
 * com.darkom.payment.cmi.CmiClient} for the same reason.
 */
public interface EmailSender {

  void send(String to, String subject, String body);
}
