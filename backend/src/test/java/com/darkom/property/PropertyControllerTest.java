package com.darkom.property;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class PropertyControllerTest extends AbstractIntegrationTest {

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

  private String propertyJson(String name) {
    return """
        {"name":"%s","address":"12 Rue des Oliviers","city":"Rabat"}
        """
        .formatted(name);
  }

  @Test
  void landlordCreatesAndListsOwnProperty() throws Exception {
    String token = registerAndLogin("landlord1@example.com", "LANDLORD");

    mockMvc
        .perform(
            post("/api/v1/properties")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(propertyJson("Villa Zaytouna")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Villa Zaytouna"))
        .andExpect(jsonPath("$.archived").value(false));

    mockMvc
        .perform(get("/api/v1/properties").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Villa Zaytouna"));
  }

  @Test
  void getReturnsOwnPropertyButNotFoundForAnotherLandlords() throws Exception {
    String ownerToken = registerAndLogin("owner3@example.com", "LANDLORD");
    String otherToken = registerAndLogin("other3@example.com", "LANDLORD");

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/properties")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(propertyJson("Villa Zaytouna")))
            .andReturn();
    String propertyId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            get("/api/v1/properties/" + propertyId).header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Villa Zaytouna"));

    mockMvc
        .perform(
            get("/api/v1/properties/" + propertyId).header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void tenantCannotCreateOrListProperties() throws Exception {
    String token = registerAndLogin("tenant1@example.com", "TENANT");

    mockMvc
        .perform(
            post("/api/v1/properties")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(propertyJson("Villa Zaytouna")))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/api/v1/properties").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void propertyManagerCannotCreateProperty() throws Exception {
    String token = registerAndLogin("pm1@example.com", "PROPERTY_MANAGER");

    mockMvc
        .perform(
            post("/api/v1/properties")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(propertyJson("Villa Zaytouna")))
        .andExpect(status().isForbidden());
  }

  @Test
  void landlordCannotAccessAnotherLandlordsProperty() throws Exception {
    String ownerToken = registerAndLogin("owner1@example.com", "LANDLORD");
    String otherToken = registerAndLogin("other1@example.com", "LANDLORD");

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/properties")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(propertyJson("Villa Zaytouna")))
            .andReturn();
    String propertyId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            patch("/api/v1/properties/" + propertyId)
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(propertyJson("Hijacked Name")))
        .andExpect(status().isNotFound());
  }

  @Test
  void archiveMarksPropertyArchived() throws Exception {
    String token = registerAndLogin("landlord2@example.com", "LANDLORD");

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(propertyJson("Villa Zaytouna")))
            .andReturn();
    String propertyId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            post("/api/v1/properties/" + propertyId + "/archive")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archived").value(true));
  }

  @Test
  void createsAndListsUnitsUnderAProperty() throws Exception {
    String token = registerAndLogin("landlord3@example.com", "LANDLORD");

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(propertyJson("Villa Zaytouna")))
            .andReturn();
    String propertyId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            post("/api/v1/properties/" + propertyId + "/units")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"label":"Apt 1","monthlyRent":3500}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("VACANT"));

    mockMvc
        .perform(
            get("/api/v1/properties/" + propertyId + "/units")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].label").value("Apt 1"));
  }

  @Test
  void cannotCreateUnitsUnderAnotherLandlordsProperty() throws Exception {
    String ownerToken = registerAndLogin("owner2@example.com", "LANDLORD");
    String otherToken = registerAndLogin("other2@example.com", "LANDLORD");

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/properties")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(propertyJson("Villa Zaytouna")))
            .andReturn();
    String propertyId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            post("/api/v1/properties/" + propertyId + "/units")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"label":"Apt 1","monthlyRent":3500}
                    """))
        .andExpect(status().isNotFound());
  }

  @Test
  void updatesAndArchivesAUnit() throws Exception {
    String token = registerAndLogin("landlord4@example.com", "LANDLORD");

    MvcResult propertyResult =
        mockMvc
            .perform(
                post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(propertyJson("Villa Zaytouna")))
            .andReturn();
    String propertyId = JsonPath.read(propertyResult.getResponse().getContentAsString(), "$.id");

    MvcResult unitResult =
        mockMvc
            .perform(
                post("/api/v1/properties/" + propertyId + "/units")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"label":"Apt 1","monthlyRent":3500}
                        """))
            .andReturn();
    String unitId = JsonPath.read(unitResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            patch("/api/v1/units/" + unitId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"label":"Apt 1 Renamed","monthlyRent":4200}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Apt 1 Renamed"));

    mockMvc
        .perform(
            post("/api/v1/units/" + unitId + "/archive").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archived").value(true));
  }

  @Test
  void rejectsUnitCreationWithNonPositiveRent() throws Exception {
    String token = registerAndLogin("landlord5@example.com", "LANDLORD");

    MvcResult propertyResult =
        mockMvc
            .perform(
                post("/api/v1/properties")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(propertyJson("Villa Zaytouna")))
            .andReturn();
    String propertyId = JsonPath.read(propertyResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            post("/api/v1/properties/" + propertyId + "/units")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"label":"Apt 1","monthlyRent":0}
                    """))
        .andExpect(status().isBadRequest());
  }
}
