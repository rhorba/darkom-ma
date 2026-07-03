package com.darkom.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private final JwtProperties properties = new JwtProperties();
  private final JwtService jwtService;

  JwtServiceTest() {
    properties.setSecret("test-only-secret-key-at-least-32-characters-long");
    properties.setAccessTokenTtlMinutes(15);
    properties.setRefreshTokenTtlDays(7);
    this.jwtService = new JwtService(properties);
  }

  private User user(Role role) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail("test@example.com");
    user.setRole(role);
    return user;
  }

  @Test
  void generatesAndParsesAccessTokenRoundTrip() {
    User user = user(Role.LANDLORD);

    String token = jwtService.generateAccessToken(user);
    Claims claims = jwtService.parseAccessToken(token);

    assertThat(jwtService.extractUserId(claims)).isEqualTo(user.getId());
    assertThat(jwtService.extractRole(claims)).isEqualTo(Role.LANDLORD);
  }

  @Test
  void rejectsTamperedToken() {
    User user = user(Role.TENANT);
    String token = jwtService.generateAccessToken(user);
    String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

    assertThatThrownBy(() -> jwtService.parseAccessToken(tampered))
        .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
  }

  @Test
  void rejectsExpiredToken() {
    JwtProperties expiredProps = new JwtProperties();
    expiredProps.setSecret("test-only-secret-key-at-least-32-characters-long");
    expiredProps.setAccessTokenTtlMinutes(-1);
    expiredProps.setRefreshTokenTtlDays(7);
    JwtService expiredService = new JwtService(expiredProps);

    String token = expiredService.generateAccessToken(user(Role.ADMIN));

    assertThatThrownBy(() -> expiredService.parseAccessToken(token))
        .isInstanceOf(ExpiredJwtException.class);
  }

  @Test
  void accessTokenTtlSecondsMatchesConfiguredMinutes() {
    assertThat(jwtService.accessTokenTtlSeconds()).isEqualTo(15 * 60L);
  }
}
