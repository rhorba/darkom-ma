package com.darkom.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.darkom.common.AbstractIntegrationTest;
import com.darkom.payment.cmi.CmiClient;
import com.darkom.payment.dto.CmiCallbackStatus;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class PaymentControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CmiClient cmiClient;

  private String registerAndLogin(String email, String role) throws Exception {
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"email":"%s","password":"supersecretpw","fullName":"Test User","phone":"0600000000","role":"%s"}
                """
                    .formatted(email, role)));

    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"supersecretpw"}
                        """
                            .formatted(email)))
            .andReturn();
    return JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
  }

  /** Registers a landlord + tenant, creates a property/unit/lease. Returns the lease id. */
  private String createLeaseWithPendingPayment(String landlordToken, String tenantEmail)
      throws Exception {
    MvcResult propertyResult =
        mockMvc
            .perform(
                post("/api/v1/properties")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Villa Zaytouna","address":"12 Rue des Oliviers","city":"Rabat"}
                        """))
            .andReturn();
    String propertyId = JsonPath.read(propertyResult.getResponse().getContentAsString(), "$.id");

    MvcResult unitResult =
        mockMvc
            .perform(
                post("/api/v1/properties/" + propertyId + "/units")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"label":"Apt 1","monthlyRent":3500}
                        """))
            .andReturn();
    String unitId = JsonPath.read(unitResult.getResponse().getContentAsString(), "$.id");

    MvcResult leaseResult =
        mockMvc
            .perform(
                post("/api/v1/leases")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"unitId":"%s","tenantEmail":"%s","startDate":"2026-01-01","endDate":"2026-12-31","monthlyRent":3500}
                        """
                            .formatted(unitId, tenantEmail)))
            .andReturn();
    return JsonPath.read(leaseResult.getResponse().getContentAsString(), "$.id");
  }

  private String initiatePayment(String leaseId, String tenantToken) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/payments/" + leaseId + "/initiate")
                .header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String callbackJson(
      String transactionId, CmiCallbackStatus status, BigDecimal amount, String signature) {
    return """
        {"cmiTransactionId":"%s","status":"%s","amount":%s,"signature":"%s"}
        """
        .formatted(transactionId, status, amount, signature);
  }

  @Test
  void tenantInitiatesPaymentForOwnLease() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLeaseWithPendingPayment(landlordToken, tenantEmail);

    mockMvc
        .perform(
            post("/api/v1/payments/" + leaseId + "/initiate")
                .header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cmiTransactionId").isNotEmpty())
        .andExpect(
            jsonPath("$.redirectUrl")
                .value(org.hamcrest.Matchers.containsString("/mock-cmi/pay/")));
  }

  @Test
  void landlordCannotInitiatePaymentForTenantsLease() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLeaseWithPendingPayment(landlordToken, tenantEmail);

    mockMvc
        .perform(
            post("/api/v1/payments/" + leaseId + "/initiate")
                .header("Authorization", "Bearer " + landlordToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void anotherTenantCannotInitiatePaymentForSomeoneElsesLease() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String otherTenantToken = registerAndLogin(uniqueEmail("other-tenant"), "TENANT");
    registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLeaseWithPendingPayment(landlordToken, tenantEmail);

    mockMvc
        .perform(
            post("/api/v1/payments/" + leaseId + "/initiate")
                .header("Authorization", "Bearer " + otherTenantToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void validSignedSuccessCallbackMarksPaymentPaid() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLeaseWithPendingPayment(landlordToken, tenantEmail);
    String initiation = initiatePayment(leaseId, tenantToken);
    String transactionId = JsonPath.read(initiation, "$.cmiTransactionId");

    BigDecimal amount = new BigDecimal("3500.00");
    String signature = cmiClient.sign(transactionId, amount, CmiCallbackStatus.SUCCESS);

    mockMvc
        .perform(
            post("/api/v1/payments/cmi/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackJson(transactionId, CmiCallbackStatus.SUCCESS, amount, signature)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.paidAt").isNotEmpty());
  }

  @Test
  void forgedCallbackSignatureIsRejectedAndPaymentUnchanged() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLeaseWithPendingPayment(landlordToken, tenantEmail);
    String initiation = initiatePayment(leaseId, tenantToken);
    String transactionId = JsonPath.read(initiation, "$.cmiTransactionId");

    mockMvc
        .perform(
            post("/api/v1/payments/cmi/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    callbackJson(
                        transactionId,
                        CmiCallbackStatus.SUCCESS,
                        new BigDecimal("3500.00"),
                        "forged-signature")))
        .andExpect(status().isBadRequest());

    // A follow-up with the correct signature must still succeed - proves the forged attempt
    // above did not change the payment's state.
    String realSignature =
        cmiClient.sign(transactionId, new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS);
    mockMvc
        .perform(
            post("/api/v1/payments/cmi/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    callbackJson(
                        transactionId,
                        CmiCallbackStatus.SUCCESS,
                        new BigDecimal("3500.00"),
                        realSignature)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"));
  }

  @Test
  void replayedValidCallbackIsRejected() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLeaseWithPendingPayment(landlordToken, tenantEmail);
    String initiation = initiatePayment(leaseId, tenantToken);
    String transactionId = JsonPath.read(initiation, "$.cmiTransactionId");

    BigDecimal amount = new BigDecimal("3500.00");
    String signature = cmiClient.sign(transactionId, amount, CmiCallbackStatus.SUCCESS);
    String body = callbackJson(transactionId, CmiCallbackStatus.SUCCESS, amount, signature);

    mockMvc
        .perform(
            post("/api/v1/payments/cmi/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/payments/cmi/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void callbackForUnknownTransactionIsRejected() throws Exception {
    String signature =
        cmiClient.sign("does-not-exist", new BigDecimal("3500.00"), CmiCallbackStatus.SUCCESS);

    mockMvc
        .perform(
            post("/api/v1/payments/cmi/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    callbackJson(
                        "does-not-exist",
                        CmiCallbackStatus.SUCCESS,
                        new BigDecimal("3500.00"),
                        signature)))
        .andExpect(status().isNotFound());
  }

  @Test
  void callbackWithMismatchedAmountIsRejected() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLeaseWithPendingPayment(landlordToken, tenantEmail);
    String initiation = initiatePayment(leaseId, tenantToken);
    String transactionId = JsonPath.read(initiation, "$.cmiTransactionId");

    BigDecimal wrongAmount = new BigDecimal("1.00");
    String signature = cmiClient.sign(transactionId, wrongAmount, CmiCallbackStatus.SUCCESS);

    mockMvc
        .perform(
            post("/api/v1/payments/cmi/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    callbackJson(transactionId, CmiCallbackStatus.SUCCESS, wrongAmount, signature)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void validFailedCallbackMarksPaymentFailed() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLeaseWithPendingPayment(landlordToken, tenantEmail);
    String initiation = initiatePayment(leaseId, tenantToken);
    String transactionId = JsonPath.read(initiation, "$.cmiTransactionId");

    BigDecimal amount = new BigDecimal("3500.00");
    String signature = cmiClient.sign(transactionId, amount, CmiCallbackStatus.FAILED);

    mockMvc
        .perform(
            post("/api/v1/payments/cmi/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackJson(transactionId, CmiCallbackStatus.FAILED, amount, signature)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FAILED"));
  }
}
