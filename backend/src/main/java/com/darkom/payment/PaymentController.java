package com.darkom.payment;

import com.darkom.payment.dto.CmiCallbackRequest;
import com.darkom.payment.dto.PaymentInitiationResponse;
import com.darkom.payment.dto.PaymentResponse;
import com.darkom.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/{leaseId}/initiate")
  public PaymentInitiationResponse initiate(
      @PathVariable UUID leaseId, @AuthenticationPrincipal UUID userId) {
    return paymentService.initiate(leaseId, userId);
  }

  @PostMapping("/cmi/callback")
  public PaymentResponse callback(@Valid @RequestBody CmiCallbackRequest request) {
    return paymentService.handleCallback(request);
  }
}
