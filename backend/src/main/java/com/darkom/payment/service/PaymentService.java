package com.darkom.payment.service;

import com.darkom.lease.dto.LeaseResponse;
import com.darkom.lease.service.LeaseService;
import com.darkom.payment.cmi.CmiClient;
import com.darkom.payment.dto.CmiCallbackRequest;
import com.darkom.payment.dto.CmiCallbackStatus;
import com.darkom.payment.dto.PaymentInitiationResponse;
import com.darkom.payment.dto.PaymentResponse;
import com.darkom.payment.entity.Payment;
import com.darkom.payment.entity.PaymentStatus;
import com.darkom.payment.exception.CallbackAmountMismatchException;
import com.darkom.payment.exception.InvalidCmiSignatureException;
import com.darkom.payment.exception.NoPendingPaymentException;
import com.darkom.payment.exception.PaymentAlreadyProcessedException;
import com.darkom.payment.exception.PaymentNotAuthorizedException;
import com.darkom.payment.exception.PaymentNotFoundException;
import com.darkom.payment.repository.PaymentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  private final PaymentRepository paymentRepository;
  private final LeaseService leaseService;
  private final CmiClient cmiClient;
  private final Clock clock;

  public PaymentService(
      PaymentRepository paymentRepository,
      LeaseService leaseService,
      CmiClient cmiClient,
      Clock clock) {
    this.paymentRepository = paymentRepository;
    this.leaseService = leaseService;
    this.cmiClient = cmiClient;
    this.clock = clock;
  }

  @Transactional
  public PaymentInitiationResponse initiate(UUID leaseId, UUID currentUserId) {
    LeaseResponse lease = leaseService.get(leaseId, currentUserId);
    if (!lease.tenantId().equals(currentUserId)) {
      throw new PaymentNotAuthorizedException();
    }

    List<Payment> pending =
        paymentRepository.findAllByLeaseIdAndStatusOrderByDueDateAsc(
            leaseId, PaymentStatus.PENDING);
    Payment payment =
        pending.stream().findFirst().orElseThrow(() -> new NoPendingPaymentException(leaseId));

    String transactionId = UUID.randomUUID().toString();
    payment.setCmiTransactionId(transactionId);
    payment.setUpdatedAt(clock.instant());
    paymentRepository.save(payment);

    return new PaymentInitiationResponse(
        payment.getId(), transactionId, cmiClient.buildRedirectUrl(transactionId));
  }

  @Transactional
  public PaymentResponse handleCallback(CmiCallbackRequest request) {
    Payment payment =
        paymentRepository
            .findByCmiTransactionId(request.cmiTransactionId())
            .orElseThrow(
                () ->
                    new PaymentNotFoundException(
                        "Unknown CMI transaction: " + request.cmiTransactionId()));

    boolean signatureValid =
        cmiClient.verifySignature(
            request.cmiTransactionId(), request.amount(), request.status(), request.signature());
    if (!signatureValid) {
      log.warn(
          "Rejected CMI callback with invalid signature for transaction {}",
          request.cmiTransactionId());
      throw new InvalidCmiSignatureException();
    }

    if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.FAILED) {
      log.warn("Rejected replayed CMI callback for transaction {}", request.cmiTransactionId());
      throw new PaymentAlreadyProcessedException();
    }

    if (payment.getAmount().compareTo(request.amount()) != 0) {
      log.warn(
          "Rejected CMI callback for transaction {} - amount mismatch (expected {}, got {})",
          request.cmiTransactionId(),
          payment.getAmount(),
          request.amount());
      throw new CallbackAmountMismatchException();
    }

    Instant now = clock.instant();
    if (request.status() == CmiCallbackStatus.SUCCESS) {
      payment.setStatus(PaymentStatus.PAID);
      payment.setPaidAt(now);
    } else {
      payment.setStatus(PaymentStatus.FAILED);
    }
    payment.setUpdatedAt(now);
    paymentRepository.save(payment);

    return PaymentResponse.from(payment);
  }
}
