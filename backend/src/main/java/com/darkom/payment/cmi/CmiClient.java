package com.darkom.payment.cmi;

import com.darkom.payment.dto.CmiCallbackStatus;
import java.math.BigDecimal;

/**
 * Boundary around the CMI payment gateway, per docs/prd-darkom.md's explicit risk mitigation:
 * "Build payment flow behind an interface so a stub/mock can unblock other work while CMI access is
 * pending." No real CMI merchant account exists yet (onboarding in Morocco requires a bank
 * relationship the user doesn't have set up), so {@link MockCmiClient} is the only implementation.
 *
 * <p>The signature scheme here (HMAC-SHA256 over transactionId|amount|status) is our own design,
 * not CMI's real one - CMI's actual API docs aren't available. When real credentials/docs exist,
 * only this interface's implementation needs to change; PaymentService and everything above it
 * depends solely on this contract.
 */
public interface CmiClient {

  /** URL the tenant is redirected to in order to complete payment on the (mocked) gateway. */
  String buildRedirectUrl(String cmiTransactionId);

  /** Computes the signature CMI would attach to a callback for this transaction/amount/status. */
  String sign(String cmiTransactionId, BigDecimal amount, CmiCallbackStatus status);

  /** Verifies a callback's signature in constant time. */
  boolean verifySignature(
      String cmiTransactionId,
      BigDecimal amount,
      CmiCallbackStatus status,
      String providedSignature);
}
