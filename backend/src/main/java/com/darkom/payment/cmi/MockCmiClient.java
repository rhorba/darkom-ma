package com.darkom.payment.cmi;

import com.darkom.payment.dto.CmiCallbackStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class MockCmiClient implements CmiClient {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final CmiProperties properties;

  public MockCmiClient(CmiProperties properties) {
    this.properties = properties;
  }

  @Override
  public String buildRedirectUrl(String cmiTransactionId) {
    URI callback = URI.create(properties.getCallbackUrl());
    String authority = callback.getScheme() + "://" + callback.getAuthority();
    return authority + "/mock-cmi/pay/" + cmiTransactionId;
  }

  @Override
  public String sign(String cmiTransactionId, BigDecimal amount, CmiCallbackStatus status) {
    String message = canonicalMessage(cmiTransactionId, amount, status);
    return hmacHex(message);
  }

  @Override
  public boolean verifySignature(
      String cmiTransactionId,
      BigDecimal amount,
      CmiCallbackStatus status,
      String providedSignature) {
    if (providedSignature == null) {
      return false;
    }
    String expected = sign(cmiTransactionId, amount, status);
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        providedSignature.getBytes(StandardCharsets.UTF_8));
  }

  private String canonicalMessage(
      String cmiTransactionId, BigDecimal amount, CmiCallbackStatus status) {
    return cmiTransactionId + "|" + amount.setScale(2, RoundingMode.HALF_UP) + "|" + status.name();
  }

  private String hmacHex(String message) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(
          new SecretKeySpec(
              properties.getSecretKey().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Unable to compute CMI signature", e);
    }
  }
}
