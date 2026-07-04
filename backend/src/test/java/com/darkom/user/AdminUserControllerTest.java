package com.darkom.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.common.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class AdminUserControllerTest extends AbstractIntegrationTest {

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

  @Test
  void adminListsAllUsers() throws Exception {
    String tenantEmail = uniqueEmail("tenant");
    registerAndLogin(tenantEmail, "TENANT");
    String adminToken = createAdminAndLogin(uniqueEmail("admin"));

    mockMvc
        .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$", org.hamcrest.Matchers.hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(2))));
  }

  @Test
  void nonAdminCannotListUsers() throws Exception {
    String landlordToken = registerAndLogin(uniqueEmail("landlord"), "LANDLORD");

    mockMvc
        .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + landlordToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminDeactivatesAUserAndTheyCanNoLongerLogIn() throws Exception {
    String tenantEmail = uniqueEmail("tenant-deactivate");
    registerAndLogin(tenantEmail, "TENANT");
    String adminToken = createAdminAndLogin(uniqueEmail("admin2"));

    MvcResult listResult =
        mockMvc
            .perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
            .andReturn();
    net.minidev.json.JSONArray matchingIds =
        JsonPath.read(
            listResult.getResponse().getContentAsString(),
            "$[?(@.email=='" + tenantEmail + "')].id");
    String userId = (String) matchingIds.get(0);

    mockMvc
        .perform(
            patch("/api/v1/admin/users/" + userId + "/deactivate")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"supersecretpw"}
                    """
                        .formatted(tenantEmail)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deactivateThrowsForAnUnknownUser() throws Exception {
    String adminToken = createAdminAndLogin(uniqueEmail("admin3"));

    mockMvc
        .perform(
            patch("/api/v1/admin/users/" + UUID.randomUUID() + "/deactivate")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }
}
