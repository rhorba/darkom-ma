package com.darkom.payment.cmi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.darkom.common.AbstractIntegrationTest;
import com.darkom.payment.entity.Payment;
import com.darkom.payment.entity.PaymentStatus;
import com.darkom.payment.repository.PaymentRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class MockCmiControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private PaymentRepository paymentRepository;

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

  private String initiateAndGetTransactionId(String landlordToken, String tenantEmail)
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
    String leaseId = JsonPath.read(leaseResult.getResponse().getContentAsString(), "$.id");
    String tenantToken = login(tenantEmail);

    MvcResult initiateResult =
        mockMvc
            .perform(
                post("/api/v1/payments/" + leaseId + "/initiate")
                    .header("Authorization", "Bearer " + tenantToken))
            .andReturn();
    return JsonPath.read(initiateResult.getResponse().getContentAsString(), "$.cmiTransactionId");
  }

  private String login(String email) throws Exception {
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

  @Test
  void payPageShows404ForAnUnknownTransaction() throws Exception {
    mockMvc.perform(get("/mock-cmi/pay/" + UUID.randomUUID())).andExpect(status().isNotFound());
  }

  @Test
  void payPageRendersTheAmountForAKnownTransaction() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String transactionId = initiateAndGetTransactionId(landlordToken, tenantEmail);

    mockMvc
        .perform(get("/mock-cmi/pay/" + transactionId))
        .andExpect(status().isOk())
        .andExpect(
            result -> assertThat(result.getResponse().getContentAsString()).contains("3500"));
  }

  @Test
  void succeedMarksThePaymentPaidAndRedirectsToTheFrontend() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String transactionId = initiateAndGetTransactionId(landlordToken, tenantEmail);

    mockMvc
        .perform(post("/mock-cmi/pay/" + transactionId + "/succeed"))
        .andExpect(status().isFound())
        .andExpect(
            result ->
                assertThat(result.getResponse().getRedirectedUrl())
                    .endsWith("/my-lease?payment=success"));

    Payment payment = paymentRepository.findByCmiTransactionId(transactionId).orElseThrow();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
  }

  @Test
  void failMarksThePaymentFailedAndRedirectsToTheFrontend() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String transactionId = initiateAndGetTransactionId(landlordToken, tenantEmail);

    mockMvc
        .perform(post("/mock-cmi/pay/" + transactionId + "/fail"))
        .andExpect(status().isFound())
        .andExpect(
            result ->
                assertThat(result.getResponse().getRedirectedUrl())
                    .endsWith("/my-lease?payment=failed"));

    Payment payment = paymentRepository.findByCmiTransactionId(transactionId).orElseThrow();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
  }
}
