package com.darkom.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.darkom.lease.dto.LeaseResponse;
import com.darkom.lease.entity.LeaseStatus;
import com.darkom.lease.service.LeaseService;
import com.darkom.payment.cmi.CmiClient;
import com.darkom.payment.dto.CmiCallbackRequest;
import com.darkom.payment.dto.CmiCallbackStatus;
import com.darkom.payment.entity.Payment;
import com.darkom.payment.entity.PaymentStatus;
import com.darkom.payment.exception.CallbackAmountMismatchException;
import com.darkom.payment.exception.InvalidCmiSignatureException;
import com.darkom.payment.exception.NoPendingPaymentException;
import com.darkom.payment.exception.PaymentAlreadyProcessedException;
import com.darkom.payment.exception.PaymentNotAuthorizedException;
import com.darkom.payment.exception.PaymentNotFoundException;
import com.darkom.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private LeaseService leaseService;
  @Mock private CmiClient cmiClient;

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private PaymentService paymentService;

  @BeforeEach
  void setUp() {
    paymentService = new PaymentService(paymentRepository, leaseService, cmiClient, clock);
  }

  private LeaseResponse lease(UUID id, UUID tenantId) {
    return new LeaseResponse(
        id,
        UUID.randomUUID(),
        tenantId,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        new BigDecimal("3500.00"),
        LeaseStatus.ACTIVE);
  }

  private Payment pendingPayment(UUID leaseId, BigDecimal amount) {
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID());
    payment.setLeaseId(leaseId);
    payment.setAmount(amount);
    payment.setDueDate(LocalDate.of(2026, 1, 1));
    payment.setStatus(PaymentStatus.PENDING);
    return payment;
  }

  @Test
  void initiateRejectsCallerWhoIsNotTheLeasesTenant() {
    UUID leaseId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    UUID landlordId = UUID.randomUUID();
    when(leaseService.get(leaseId, landlordId)).thenReturn(lease(leaseId, tenantId));

    assertThatThrownBy(() -> paymentService.initiate(leaseId, landlordId))
        .isInstanceOf(PaymentNotAuthorizedException.class);
  }

  @Test
  void initiateThrowsWhenNoPendingPaymentExists() {
    UUID leaseId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    when(leaseService.get(leaseId, tenantId)).thenReturn(lease(leaseId, tenantId));
    when(paymentRepository.findAllByLeaseIdAndStatusOrderByDueDateAsc(
            leaseId, PaymentStatus.PENDING))
        .thenReturn(List.of());

    assertThatThrownBy(() -> paymentService.initiate(leaseId, tenantId))
        .isInstanceOf(NoPendingPaymentException.class);
  }

  @Test
  void initiateStoresTransactionIdAndReturnsRedirectUrl() {
    UUID leaseId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Payment payment = pendingPayment(leaseId, new BigDecimal("3500.00"));
    when(leaseService.get(leaseId, tenantId)).thenReturn(lease(leaseId, tenantId));
    when(paymentRepository.findAllByLeaseIdAndStatusOrderByDueDateAsc(
            leaseId, PaymentStatus.PENDING))
        .thenReturn(List.of(payment));
    when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(cmiClient.buildRedirectUrl(any())).thenReturn("http://localhost:8080/mock-cmi/pay/txn");

    var result = paymentService.initiate(leaseId, tenantId);

    assertThat(result.paymentId()).isEqualTo(payment.getId());
    assertThat(result.cmiTransactionId()).isNotBlank();
    assertThat(result.redirectUrl()).isEqualTo("http://localhost:8080/mock-cmi/pay/txn");
    assertThat(payment.getCmiTransactionId()).isEqualTo(result.cmiTransactionId());
  }

  private CmiCallbackRequest callback(
      String txn, CmiCallbackStatus status, BigDecimal amount, String sig) {
    return new CmiCallbackRequest(txn, status, amount, sig);
  }

  @Test
  void callbackThrowsWhenTransactionIsUnknown() {
    when(paymentRepository.findByCmiTransactionId("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                paymentService.handleCallback(
                    callback(
                        "unknown", CmiCallbackStatus.SUCCESS, new BigDecimal("3500.00"), "sig")))
        .isInstanceOf(PaymentNotFoundException.class);
  }

  @Test
  void callbackRejectsInvalidSignatureAndLeavesPaymentUnchanged() {
    Payment payment = pendingPayment(UUID.randomUUID(), new BigDecimal("3500.00"));
    payment.setCmiTransactionId("txn-1");
    when(paymentRepository.findByCmiTransactionId("txn-1")).thenReturn(Optional.of(payment));
    when(cmiClient.verifySignature(any(), any(), any(), any())).thenReturn(false);

    assertThatThrownBy(
            () ->
                paymentService.handleCallback(
                    callback(
                        "txn-1", CmiCallbackStatus.SUCCESS, new BigDecimal("3500.00"), "bad-sig")))
        .isInstanceOf(InvalidCmiSignatureException.class);

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.getPaidAt()).isNull();
  }

  @Test
  void callbackRejectsAReplayOnAnAlreadyPaidPayment() {
    Payment payment = pendingPayment(UUID.randomUUID(), new BigDecimal("3500.00"));
    payment.setCmiTransactionId("txn-1");
    payment.setStatus(PaymentStatus.PAID);
    payment.setPaidAt(Instant.parse("2026-01-01T00:00:00Z"));
    when(paymentRepository.findByCmiTransactionId("txn-1")).thenReturn(Optional.of(payment));
    when(cmiClient.verifySignature(any(), any(), any(), any())).thenReturn(true);

    assertThatThrownBy(
            () ->
                paymentService.handleCallback(
                    callback("txn-1", CmiCallbackStatus.SUCCESS, new BigDecimal("3500.00"), "sig")))
        .isInstanceOf(PaymentAlreadyProcessedException.class);
  }

  @Test
  void callbackRejectsAnAmountMismatch() {
    Payment payment = pendingPayment(UUID.randomUUID(), new BigDecimal("3500.00"));
    payment.setCmiTransactionId("txn-1");
    when(paymentRepository.findByCmiTransactionId("txn-1")).thenReturn(Optional.of(payment));
    when(cmiClient.verifySignature(any(), any(), any(), any())).thenReturn(true);

    assertThatThrownBy(
            () ->
                paymentService.handleCallback(
                    callback("txn-1", CmiCallbackStatus.SUCCESS, new BigDecimal("1.00"), "sig")))
        .isInstanceOf(CallbackAmountMismatchException.class);

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
  }

  @Test
  void callbackMarksPaymentPaidOnValidSuccessCallback() {
    Payment payment = pendingPayment(UUID.randomUUID(), new BigDecimal("3500.00"));
    payment.setCmiTransactionId("txn-1");
    when(paymentRepository.findByCmiTransactionId("txn-1")).thenReturn(Optional.of(payment));
    when(cmiClient.verifySignature(any(), any(), any(), any())).thenReturn(true);
    when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        paymentService.handleCallback(
            callback("txn-1", CmiCallbackStatus.SUCCESS, new BigDecimal("3500.00"), "sig"));

    assertThat(result.status()).isEqualTo(PaymentStatus.PAID);
    assertThat(payment.getPaidAt()).isEqualTo(clock.instant());
  }

  @Test
  void callbackMarksPaymentFailedOnValidFailedCallback() {
    Payment payment = pendingPayment(UUID.randomUUID(), new BigDecimal("3500.00"));
    payment.setCmiTransactionId("txn-1");
    when(paymentRepository.findByCmiTransactionId("txn-1")).thenReturn(Optional.of(payment));
    when(cmiClient.verifySignature(any(), any(), any(), any())).thenReturn(true);
    when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        paymentService.handleCallback(
            callback("txn-1", CmiCallbackStatus.FAILED, new BigDecimal("3500.00"), "sig"));

    assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
    assertThat(payment.getPaidAt()).isNull();
  }
}
