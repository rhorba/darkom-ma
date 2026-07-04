package com.darkom.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * MockMvc never exercises this: Spring Boot's default error handling forwards a rejected request to
 * an internal GET /error dispatch to render the JSON body. Without permitting /error in
 * SecurityConfig, that forward is itself re-evaluated as an anonymous, unauthenticated request, and
 * its AuthenticationEntryPoint overwrites the already-correct 403 with 401 - silently breaking
 * every role-restricted endpoint's status code on a real server while every MockMvc-based test
 * still reports 403. Only a real embedded server (RANDOM_PORT + a real HTTP client) reproduces it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityErrorResponseTest extends AbstractIntegrationTest {

  @LocalServerPort private int port;

  @Test
  void wrongRoleOnARealEmbeddedServerReturnsForbiddenNotUnauthorized() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    String baseUrl = "http://localhost:" + port;
    String email = uniqueEmail("diag-pm");

    client.send(
        HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/register"))
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "{\"email\":\"%s\",\"password\":\"supersecretpw\",\"fullName\":\"Diag\",\"phone\":\"0600000000\",\"role\":\"PROPERTY_MANAGER\"}"
                        .formatted(email)))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    HttpResponse<String> loginResponse =
        client.send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"%s\",\"password\":\"supersecretpw\"}".formatted(email)))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    String token = loginResponse.body().split("\"accessToken\":\"")[1].split("\"")[0];

    HttpResponse<String> response =
        client.send(
            HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/properties"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "{\"name\":\"x\",\"address\":\"y\",\"city\":\"z\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(403, response.statusCode());
  }
}
