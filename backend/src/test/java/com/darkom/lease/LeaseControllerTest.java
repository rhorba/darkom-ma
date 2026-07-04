package com.darkom.lease;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.common.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class LeaseControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String registerAndLogin(String email, String role) throws Exception {
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"email":"%s","password":"supersecretpw","fullName":"Test User","phone":"0600000000","role":"%s"}
                """
                    .formatted(email, role)));
    return login(email);
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

  /** No self-registration path exists for ADMIN by design (Story 1.1) - seed directly for tests. */
  private String createAdminAndLogin(String email) throws Exception {
    User admin = new User();
    admin.setId(UUID.randomUUID());
    admin.setEmail(email);
    admin.setPasswordHash(passwordEncoder.encode("supersecretpw"));
    admin.setFullName("Admin User");
    admin.setRole(Role.ADMIN);
    admin.setActive(true);
    admin.setCreatedAt(Instant.now());
    admin.setUpdatedAt(Instant.now());
    userRepository.save(admin);
    return login(email);
  }

  private record PropertyAndUnit(String propertyId, String unitId) {}

  private PropertyAndUnit createPropertyAndUnit(String landlordToken) throws Exception {
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
    return new PropertyAndUnit(propertyId, unitId);
  }

  private String leaseJson(String unitId, String tenantEmail, String start, String end) {
    return """
        {"unitId":"%s","tenantEmail":"%s","startDate":"%s","endDate":"%s","monthlyRent":3500}
        """
        .formatted(unitId, tenantEmail, start, end);
  }

  @Test
  void landlordCreatesLeaseForVacantUnitAndUnitBecomesOccupied() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    PropertyAndUnit propertyAndUnit = createPropertyAndUnit(landlordToken);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/leases")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        leaseJson(
                            propertyAndUnit.unitId(), tenantEmail, "2026-01-01", "2026-12-31")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();
    String leaseId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            get("/api/v1/properties/" + propertyAndUnit.propertyId() + "/units")
                .header("Authorization", "Bearer " + landlordToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("OCCUPIED"));

    MvcResult download =
        mockMvc
            .perform(
                get("/api/v1/leases/" + leaseId + "/document")
                    .header("Authorization", "Bearer " + landlordToken))
            .andExpect(status().isOk())
            .andExpect(
                result2 ->
                    assertThat(result2.getResponse().getContentType()).isEqualTo("application/pdf"))
            .andReturn();
    byte[] pdfBytes = download.getResponse().getContentAsByteArray();
    assertThat(pdfBytes).isNotEmpty();
    assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void rejectsLeasingAnAlreadyOccupiedUnit() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenant1Email = uniqueEmail("tenant1");
    String tenant2Email = uniqueEmail("tenant2");
    registerAndLogin(tenant1Email, "TENANT");
    registerAndLogin(tenant2Email, "TENANT");
    String unitId = createPropertyAndUnit(landlordToken).unitId();

    mockMvc
        .perform(
            post("/api/v1/leases")
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaseJson(unitId, tenant1Email, "2026-01-01", "2026-12-31")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/leases")
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaseJson(unitId, tenant2Email, "2026-01-01", "2026-12-31")))
        .andExpect(status().isConflict());
  }

  @Test
  void rejectsEndDateBeforeStartDate() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String unitId = createPropertyAndUnit(landlordToken).unitId();

    mockMvc
        .perform(
            post("/api/v1/leases")
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaseJson(unitId, tenantEmail, "2026-12-31", "2026-01-01")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsUnknownTenantEmail() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String unitId = createPropertyAndUnit(landlordToken).unitId();

    mockMvc
        .perform(
            post("/api/v1/leases")
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    leaseJson(
                        unitId,
                        "nobody-" + UUID.randomUUID() + "@example.com",
                        "2026-01-01",
                        "2026-12-31")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void tenantRoleCannotCreateLeases() throws Exception {
    String tenantToken = registerAndLogin(uniqueEmail("tenant"), "TENANT");

    mockMvc
        .perform(
            post("/api/v1/leases")
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    leaseJson(
                        UUID.randomUUID().toString(), "x@example.com", "2026-01-01", "2026-12-31")))
        .andExpect(status().isForbidden());
  }

  @Test
  void tenantOnTheLeaseCanReadItButAnotherTenantCannot() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String otherTenantEmail = uniqueEmail("othertenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String otherTenantToken = registerAndLogin(otherTenantEmail, "TENANT");
    String unitId = createPropertyAndUnit(landlordToken).unitId();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/leases")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(leaseJson(unitId, tenantEmail, "2026-01-01", "2026-12-31")))
            .andReturn();
    String leaseId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(get("/api/v1/leases/" + leaseId).header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/leases/" + leaseId).header("Authorization", "Bearer " + otherTenantToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void landlordWithoutAccessToTheUnitCannotReadTheLease() throws Exception {
    String ownerToken = registerAndLogin(uniqueEmail("owner"), "LANDLORD");
    String otherLandlordToken = registerAndLogin(uniqueEmail("other"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String unitId = createPropertyAndUnit(ownerToken).unitId();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/leases")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(leaseJson(unitId, tenantEmail, "2026-01-01", "2026-12-31")))
            .andReturn();
    String leaseId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            get("/api/v1/leases/" + leaseId)
                .header("Authorization", "Bearer " + otherLandlordToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void adminHasPlatformWideReadAccess() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String unitId = createPropertyAndUnit(landlordToken).unitId();
    String adminToken = createAdminAndLogin(uniqueEmail("admin"));

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/leases")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(leaseJson(unitId, tenantEmail, "2026-01-01", "2026-12-31")))
            .andReturn();
    String leaseId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(get("/api/v1/leases/" + leaseId).header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void mineReturnsTheTenantsActiveLeaseWithUnitAndPropertyDetails() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createPropertyAndUnit(landlordToken).unitId();

    mockMvc
        .perform(
            post("/api/v1/leases")
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaseJson(unitId, tenantEmail, "2026-01-01", "2026-12-31")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/leases/mine").header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unitLabel").value("Apt 1"))
        .andExpect(jsonPath("$.propertyName").value("Villa Zaytouna"));
  }

  @Test
  void mineReturns404WhenTenantHasNoActiveLease() throws Exception {
    String tenantToken = registerAndLogin(uniqueEmail("tenant"), "TENANT");

    mockMvc
        .perform(get("/api/v1/leases/mine").header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void paymentsListsHistoryForTheLeaseOwningTenant() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createPropertyAndUnit(landlordToken).unitId();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/leases")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(leaseJson(unitId, tenantEmail, "2026-01-01", "2026-12-31")))
            .andReturn();
    String leaseId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            get("/api/v1/leases/" + leaseId + "/payments")
                .header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  void paymentsRejectsAnUnrelatedTenant() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String otherTenantToken = registerAndLogin(uniqueEmail("other"), "TENANT");
    registerAndLogin(tenantEmail, "TENANT");
    String unitId = createPropertyAndUnit(landlordToken).unitId();

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/leases")
                    .header("Authorization", "Bearer " + landlordToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(leaseJson(unitId, tenantEmail, "2026-01-01", "2026-12-31")))
            .andReturn();
    String leaseId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            get("/api/v1/leases/" + leaseId + "/payments")
                .header("Authorization", "Bearer " + otherTenantToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void concurrentLeaseCreationOnTheSameUnitOnlyLetsOneSucceed() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenant1Email = uniqueEmail("tenant1");
    String tenant2Email = uniqueEmail("tenant2");
    registerAndLogin(tenant1Email, "TENANT");
    registerAndLogin(tenant2Email, "TENANT");
    String unitId = createPropertyAndUnit(landlordToken).unitId();

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> attempt1 =
          () ->
              mockMvc
                  .perform(
                      post("/api/v1/leases")
                          .header("Authorization", "Bearer " + landlordToken)
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(leaseJson(unitId, tenant1Email, "2026-01-01", "2026-12-31")))
                  .andReturn()
                  .getResponse()
                  .getStatus();
      Callable<Integer> attempt2 =
          () ->
              mockMvc
                  .perform(
                      post("/api/v1/leases")
                          .header("Authorization", "Bearer " + landlordToken)
                          .contentType(MediaType.APPLICATION_JSON)
                          .content(leaseJson(unitId, tenant2Email, "2026-01-01", "2026-12-31")))
                  .andReturn()
                  .getResponse()
                  .getStatus();

      List<Future<Integer>> futures = executor.invokeAll(List.of(attempt1, attempt2));
      List<Integer> statuses = futures.stream().map(this::getUnchecked).toList();

      assertThat(statuses).containsExactlyInAnyOrder(201, 409);
    } finally {
      executor.shutdown();
    }
  }

  private <T> T getUnchecked(Future<T> future) {
    try {
      return future.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
