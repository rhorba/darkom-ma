package com.darkom.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.darkom.common.AbstractIntegrationTest;
import com.darkom.payment.entity.Payment;
import com.darkom.payment.entity.PaymentStatus;
import com.darkom.payment.repository.PaymentRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class RentReminderJobIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RentReminderJob job;
  @Autowired private PaymentRepository paymentRepository;

  /** Fixes "today" so the reminder window is deterministic regardless of when the suite runs. */
  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);
    }
  }

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

  private String createLease(String landlordToken, String tenantEmail, String endDate)
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
                        {"unitId":"%s","tenantEmail":"%s","startDate":"2026-01-01","endDate":"%s","monthlyRent":3500}
                        """
                            .formatted(unitId, tenantEmail, endDate)))
            .andReturn();
    return JsonPath.read(leaseResult.getResponse().getContentAsString(), "$.id");
  }

  @Test
  void generatesNextPaymentAndSendsReminderAgainstARealDatabase() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLease(landlordToken, tenantEmail, "2027-01-01");

    // Story 3.1 creates the first payment due 2026-01-01 (the lease start date). Move it inside
    // the reminder window so this run's "generate next payment" branch actually fires, mirroring
    // time having passed since lease creation.
    List<Payment> payments =
        paymentRepository.findAllByLeaseIdAndStatusOrderByDueDateAsc(
            java.util.UUID.fromString(leaseId), PaymentStatus.PENDING);
    assertThat(payments).hasSize(1);
    Payment firstPayment = payments.get(0);
    firstPayment.setDueDate(LocalDate.of(2026, 3, 2));
    paymentRepository.save(firstPayment);

    job.runDailyJob();

    List<Payment> allPayments =
        paymentRepository.findAll().stream()
            .filter(p -> p.getLeaseId().toString().equals(leaseId))
            .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
            .toList();
    assertThat(allPayments).hasSize(2);
    assertThat(allPayments.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 4, 2));
    assertThat(allPayments.get(1).getStatus()).isEqualTo(PaymentStatus.PENDING);

    Payment reminded = paymentRepository.findById(firstPayment.getId()).orElseThrow();
    assertThat(reminded.getReminderSentAt()).isNotNull();
  }

  @Test
  void doesNotResendAReminderOnASecondRun() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String leaseId = createLease(landlordToken, tenantEmail, "2027-01-01");

    List<Payment> payments =
        paymentRepository.findAllByLeaseIdAndStatusOrderByDueDateAsc(
            java.util.UUID.fromString(leaseId), PaymentStatus.PENDING);
    Payment firstPayment = payments.get(0);
    firstPayment.setDueDate(LocalDate.of(2026, 3, 3));
    paymentRepository.save(firstPayment);

    job.runDailyJob();
    Instant firstReminderSentAt =
        paymentRepository.findById(firstPayment.getId()).orElseThrow().getReminderSentAt();
    assertThat(firstReminderSentAt).isNotNull();

    job.runDailyJob();
    Instant secondReminderSentAt =
        paymentRepository.findById(firstPayment.getId()).orElseThrow().getReminderSentAt();
    assertThat(secondReminderSentAt).isEqualTo(firstReminderSentAt);
  }
}
