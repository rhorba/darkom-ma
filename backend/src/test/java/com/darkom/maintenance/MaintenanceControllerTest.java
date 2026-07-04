package com.darkom.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.darkom.common.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class MaintenanceControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

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

  private String createLeasedUnit(String landlordToken, String tenantEmail) throws Exception {
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

    mockMvc.perform(
        post("/api/v1/leases")
            .header("Authorization", "Bearer " + landlordToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"unitId":"%s","tenantEmail":"%s","startDate":"2026-01-01","endDate":"2026-12-31","monthlyRent":3500}
                """
                    .formatted(unitId, tenantEmail)));
    return unitId;
  }

  @Test
  void tenantSubmitsARequestForTheirLeasedUnit() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createLeasedUnit(landlordToken, tenantEmail);

    mockMvc
        .perform(
            multipart("/api/v1/maintenance")
                .param("unitId", unitId)
                .param("description", "Leaking faucet")
                .header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.hasPhoto").value(false));
  }

  @Test
  void tenantSubmitsARequestWithAPhotoAndCanDownloadIt() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createLeasedUnit(landlordToken, tenantEmail);

    MockMultipartFile photo =
        new MockMultipartFile("photo", "leak.jpg", "image/jpeg", new byte[] {1, 2, 3, 4});

    MvcResult result =
        mockMvc
            .perform(
                multipart("/api/v1/maintenance")
                    .file(photo)
                    .param("unitId", unitId)
                    .param("description", "Leaking faucet")
                    .header("Authorization", "Bearer " + tenantToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.hasPhoto").value(true))
            .andReturn();
    String requestId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            get("/api/v1/maintenance/" + requestId + "/photo")
                .header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isOk())
        .andExpect(
            result2 -> assertThat(result2.getResponse().getContentType()).isEqualTo("image/jpeg"))
        .andExpect(
            result2 ->
                assertThat(result2.getResponse().getContentAsByteArray())
                    .isEqualTo(new byte[] {1, 2, 3, 4}));
  }

  @Test
  void rejectsAnUnsupportedPhotoType() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createLeasedUnit(landlordToken, tenantEmail);

    MockMultipartFile photo =
        new MockMultipartFile("photo", "notes.txt", "text/plain", "hello".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/maintenance")
                .file(photo)
                .param("unitId", unitId)
                .param("description", "Leaking faucet")
                .header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void tenantCannotSubmitForAUnitTheyDoNotLease() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String otherTenantToken = registerAndLogin(uniqueEmail("other"), "TENANT");
    String unrelatedTenantEmail = uniqueEmail("unrelated");
    registerAndLogin(unrelatedTenantEmail, "TENANT");
    String unitId = createLeasedUnit(landlordToken, unrelatedTenantEmail);

    mockMvc
        .perform(
            multipart("/api/v1/maintenance")
                .param("unitId", unitId)
                .param("description", "Leaking faucet")
                .header("Authorization", "Bearer " + otherTenantToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void landlordCannotSubmitARequest() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");

    mockMvc
        .perform(
            multipart("/api/v1/maintenance")
                .param("unitId", java.util.UUID.randomUUID().toString())
                .param("description", "Leaking faucet")
                .header("Authorization", "Bearer " + landlordToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void landlordSeesRequestsForTheirOwnPropertiesOnly() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String otherLandlordToken = registerAndLogin(uniqueEmail("other-landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createLeasedUnit(landlordToken, tenantEmail);

    mockMvc.perform(
        multipart("/api/v1/maintenance")
            .param("unitId", unitId)
            .param("description", "Leaking faucet")
            .header("Authorization", "Bearer " + tenantToken));

    mockMvc
        .perform(get("/api/v1/maintenance").header("Authorization", "Bearer " + landlordToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

    mockMvc
        .perform(get("/api/v1/maintenance").header("Authorization", "Bearer " + otherLandlordToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  void tenantSeesOnlyTheirOwnRequestsViaMine() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createLeasedUnit(landlordToken, tenantEmail);

    mockMvc.perform(
        multipart("/api/v1/maintenance")
            .param("unitId", unitId)
            .param("description", "Leaking faucet")
            .header("Authorization", "Bearer " + tenantToken));

    mockMvc
        .perform(get("/api/v1/maintenance/mine").header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
  }

  @Test
  void landlordUpdatesStatusOfARequestOnTheirProperty() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createLeasedUnit(landlordToken, tenantEmail);

    MvcResult createResult =
        mockMvc
            .perform(
                multipart("/api/v1/maintenance")
                    .param("unitId", unitId)
                    .param("description", "Leaking faucet")
                    .header("Authorization", "Bearer " + tenantToken))
            .andReturn();
    String requestId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            patch("/api/v1/maintenance/" + requestId)
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"IN_PROGRESS"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  void unrelatedLandlordCannotUpdateStatus() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String otherLandlordToken = registerAndLogin(uniqueEmail("other-landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createLeasedUnit(landlordToken, tenantEmail);

    MvcResult createResult =
        mockMvc
            .perform(
                multipart("/api/v1/maintenance")
                    .param("unitId", unitId)
                    .param("description", "Leaking faucet")
                    .header("Authorization", "Bearer " + tenantToken))
            .andReturn();
    String requestId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            patch("/api/v1/maintenance/" + requestId)
                .header("Authorization", "Bearer " + otherLandlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"IN_PROGRESS"}
                    """))
        .andExpect(status().isNotFound());
  }

  @Test
  void tenantCannotUpdateStatus() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String unitId = createLeasedUnit(landlordToken, tenantEmail);

    MvcResult createResult =
        mockMvc
            .perform(
                multipart("/api/v1/maintenance")
                    .param("unitId", unitId)
                    .param("description", "Leaking faucet")
                    .header("Authorization", "Bearer " + tenantToken))
            .andReturn();
    String requestId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            patch("/api/v1/maintenance/" + requestId)
                .header("Authorization", "Bearer " + tenantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"IN_PROGRESS"}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void anotherTenantCannotDownloadSomeoneElsesPhoto() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");
    String tenantEmail = uniqueEmail("tenant");
    String tenantToken = registerAndLogin(tenantEmail, "TENANT");
    String otherTenantToken = registerAndLogin(uniqueEmail("other"), "TENANT");
    String unitId = createLeasedUnit(landlordToken, tenantEmail);

    MockMultipartFile photo =
        new MockMultipartFile("photo", "leak.jpg", "image/jpeg", new byte[] {1, 2, 3});
    MvcResult createResult =
        mockMvc
            .perform(
                multipart("/api/v1/maintenance")
                    .file(photo)
                    .param("unitId", unitId)
                    .param("description", "Leaking faucet")
                    .header("Authorization", "Bearer " + tenantToken))
            .andReturn();
    String requestId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            get("/api/v1/maintenance/" + requestId + "/photo")
                .header("Authorization", "Bearer " + otherTenantToken))
        .andExpect(status().isNotFound());
  }
}
