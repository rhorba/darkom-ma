package com.darkom.payment.cmi;

import com.darkom.common.config.CorsProperties;
import com.darkom.payment.dto.CmiCallbackRequest;
import com.darkom.payment.dto.CmiCallbackStatus;
import com.darkom.payment.entity.Payment;
import com.darkom.payment.exception.PaymentNotFoundException;
import com.darkom.payment.repository.PaymentRepository;
import com.darkom.payment.service.PaymentService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The page {@link MockCmiClient#buildRedirectUrl} sends the tenant to - stands in for CMI's real
 * hosted payment page, which doesn't exist for us (no real merchant account). Public, like
 * /api/v1/payments/cmi/callback: a real bank page wouldn't carry our JWT either. Signs its own
 * callback the same way a real bank would sign its webhook - reusing CmiClient is legitimate here
 * since this endpoint IS the simulated bank, not a caller impersonating one.
 */
@RestController
@RequestMapping("/mock-cmi")
public class MockCmiController {

  private final PaymentRepository paymentRepository;
  private final PaymentService paymentService;
  private final CmiClient cmiClient;
  private final CorsProperties corsProperties;

  public MockCmiController(
      PaymentRepository paymentRepository,
      PaymentService paymentService,
      CmiClient cmiClient,
      CorsProperties corsProperties) {
    this.paymentRepository = paymentRepository;
    this.paymentService = paymentService;
    this.cmiClient = cmiClient;
    this.corsProperties = corsProperties;
  }

  @GetMapping(value = "/pay/{transactionId}", produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public String payPage(@PathVariable String transactionId) {
    Payment payment = findPaymentOrThrow(transactionId);

    return """
        <!doctype html>
        <html lang="fr">
        <head><meta charset="utf-8"><title>Simulateur CMI (mode test)</title></head>
        <body style="font-family: sans-serif; max-width: 480px; margin: 60px auto; text-align: center;">
          <h2>Simulateur de paiement CMI</h2>
          <p>Montant a payer : <strong>%s MAD</strong></p>
          <p style="color:#b00020;">Mode test - aucune vraie banque n'est contactee.</p>
          <form method="post" action="/mock-cmi/pay/%s/succeed" style="display:inline;">
            <button type="submit" style="padding:10px 20px;">Simuler un paiement reussi</button>
          </form>
          <form method="post" action="/mock-cmi/pay/%s/fail" style="display:inline; margin-left: 12px;">
            <button type="submit" style="padding:10px 20px;">Simuler un echec</button>
          </form>
        </body>
        </html>
        """
        .formatted(payment.getAmount(), transactionId, transactionId);
  }

  @PostMapping("/pay/{transactionId}/succeed")
  public ResponseEntity<Void> succeed(@PathVariable String transactionId) {
    return complete(transactionId, CmiCallbackStatus.SUCCESS);
  }

  @PostMapping("/pay/{transactionId}/fail")
  public ResponseEntity<Void> fail(@PathVariable String transactionId) {
    return complete(transactionId, CmiCallbackStatus.FAILED);
  }

  private ResponseEntity<Void> complete(String transactionId, CmiCallbackStatus status) {
    Payment payment = findPaymentOrThrow(transactionId);
    String signature = cmiClient.sign(transactionId, payment.getAmount(), status);
    paymentService.handleCallback(
        new CmiCallbackRequest(transactionId, status, payment.getAmount(), signature));

    String outcome = status == CmiCallbackStatus.SUCCESS ? "success" : "failed";
    URI redirect = URI.create(corsProperties.getAllowedOrigin() + "/my-lease?payment=" + outcome);
    return ResponseEntity.status(HttpStatus.FOUND).location(redirect).build();
  }

  private Payment findPaymentOrThrow(String transactionId) {
    return paymentRepository
        .findByCmiTransactionId(transactionId)
        .orElseThrow(
            () -> new PaymentNotFoundException("Unknown CMI transaction: " + transactionId));
  }
}
