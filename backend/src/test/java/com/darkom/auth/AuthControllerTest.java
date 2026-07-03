package com.darkom.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.darkom.common.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class AuthControllerTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private String registerJson(String email, String role) {
    return """
        {"email":"%s","password":"supersecretpw","fullName":"Test User","phone":"0600000000","role":"%s"}
        """
        .formatted(email, role);
  }

  private String loginJson(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }

  @Test
  void registerCreatesUserWithHashedPassword() throws Exception {
    String email = uniqueEmail("landlord1");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email, "LANDLORD")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value("LANDLORD"));
  }

  @Test
  void registerRejectsDuplicateEmail() throws Exception {
    String email = uniqueEmail("dup");

    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson(email, "TENANT")));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email, "TENANT")))
        .andExpect(status().isConflict());
  }

  @Test
  void registerRejectsAdminRole() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(uniqueEmail("wannabeadmin"), "ADMIN")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void loginReturnsJwtOnValidCredentials() throws Exception {
    String email = uniqueEmail("logintest");

    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson(email, "TENANT")));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, "supersecretpw")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.user.email").value(email))
        .andExpect(
            result -> assertThat(result.getResponse().getCookie("refresh_token")).isNotNull());
  }

  @Test
  void loginRejectsInvalidCredentialsWith401AndNoToken() throws Exception {
    String email = uniqueEmail("wrongpw");

    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson(email, "TENANT")));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, "totally-wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.accessToken").doesNotExist());
  }

  @Test
  void protectedEndpointWithoutTokenIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/does-not-exist-but-protected"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshRotatesTokenAndRejectsReuseOfOldOne() throws Exception {
    String email = uniqueEmail("refreshtest");

    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerJson(email, "TENANT")));

    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(email, "supersecretpw")))
            .andExpect(status().isOk())
            .andReturn();

    Cookie originalRefreshCookie = loginResult.getResponse().getCookie("refresh_token");
    assertThat(originalRefreshCookie).isNotNull();

    MvcResult refreshResult =
        mockMvc
            .perform(post("/api/v1/auth/refresh").cookie(originalRefreshCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();

    Cookie rotatedRefreshCookie = refreshResult.getResponse().getCookie("refresh_token");
    assertThat(rotatedRefreshCookie).isNotNull();
    assertThat(rotatedRefreshCookie.getValue()).isNotEqualTo(originalRefreshCookie.getValue());

    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(originalRefreshCookie))
        .andExpect(status().isUnauthorized());
  }
}
