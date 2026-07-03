package com.darkom.payment.cmi;

import static org.assertj.core.api.Assertions.assertThat;

import com.darkom.payment.dto.CmiCallbackStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MockCmiClientTest {

  private CmiProperties properties;
  private MockCmiClient client;

  @BeforeEach
  void setUp() {
    properties = new CmiProperties();
    properties.setSecretKey("test-only-cmi-secret-key");
    properties.setCallbackUrl("http://localhost:8080/api/v1/payments/cmi/callback");
    client = new MockCmiClient(properties);
  }

  @Test
  void redirectUrlPointsAtTheSameHostAsTheConfiguredCallback() {
    String url = client.buildRedirectUrl("txn-123");
    assertThat(url).isEqualTo("http://localhost:8080/mock-cmi/pay/txn-123");
  }

  @Test
  void verifiesItsOwnValidSignature() {
    String signature = client.sign("txn-123", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS);
    assertThat(
            client.verifySignature(
                "txn-123", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS, signature))
        .isTrue();
  }

  @Test
  void sameAmountDifferentScaleProducesTheSameSignature() {
    String signature = client.sign("txn-123", new BigDecimal("3500"), CmiCallbackStatus.SUCCESS);
    assertThat(
            client.verifySignature(
                "txn-123", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS, signature))
        .isTrue();
  }

  @Test
  void rejectsATamperedAmount() {
    String signature = client.sign("txn-123", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS);
    assertThat(
            client.verifySignature(
                "txn-123", new BigDecimal("9999.00"), CmiCallbackStatus.SUCCESS, signature))
        .isFalse();
  }

  @Test
  void rejectsATamperedStatus() {
    String signature = client.sign("txn-123", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS);
    assertThat(
            client.verifySignature(
                "txn-123", new BigDecimal("3500.00"), CmiCallbackStatus.FAILED, signature))
        .isFalse();
  }

  @Test
  void rejectsATamperedTransactionId() {
    String signature = client.sign("txn-123", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS);
    assertThat(
            client.verifySignature(
                "txn-999", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS, signature))
        .isFalse();
  }

  @Test
  void rejectsAGarbageSignature() {
    assertThat(
            client.verifySignature(
                "txn-123", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS, "not-a-signature"))
        .isFalse();
  }

  @Test
  void rejectsANullSignature() {
    assertThat(
            client.verifySignature(
                "txn-123", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS, null))
        .isFalse();
  }
}
