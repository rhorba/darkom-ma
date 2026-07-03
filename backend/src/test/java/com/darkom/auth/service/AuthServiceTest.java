package com.darkom.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.darkom.auth.dto.LoginRequest;
import com.darkom.auth.dto.RegisterRequest;
import com.darkom.auth.entity.RefreshToken;
import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.exception.EmailAlreadyInUseException;
import com.darkom.auth.exception.InvalidCredentialsException;
import com.darkom.auth.exception.InvalidRefreshTokenException;
import com.darkom.auth.exception.InvalidRegistrationRoleException;
import com.darkom.auth.repository.RefreshTokenRepository;
import com.darkom.auth.repository.UserRepository;
import com.darkom.common.security.JwtProperties;
import com.darkom.common.security.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private JwtService jwtService;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-only-secret-key-at-least-32-characters-long");
    properties.setAccessTokenTtlMinutes(15);
    properties.setRefreshTokenTtlDays(7);
    jwtService = new JwtService(properties);
    authService =
        new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService, clock);
  }

  private RegisterRequest registerRequest(Role role) {
    return new RegisterRequest(
        "user@example.com", "supersecretpw", "Test User", "0600000000", role);
  }

  @Test
  void registerRejectsAdminRole() {
    assertThatThrownBy(() -> authService.register(registerRequest(Role.ADMIN)))
        .isInstanceOf(InvalidRegistrationRoleException.class);
  }

  @Test
  void registerRejectsDuplicateEmail() {
    when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(registerRequest(Role.TENANT)))
        .isInstanceOf(EmailAlreadyInUseException.class);
  }

  @Test
  void registerHashesPasswordAndSavesUser() {
    when(userRepository.existsByEmail(any())).thenReturn(false);
    when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var summary = authService.register(registerRequest(Role.LANDLORD));

    assertThat(summary.email()).isEqualTo("user@example.com");
    assertThat(summary.role()).isEqualTo(Role.LANDLORD);
  }

  private User existingUser(String rawPassword, Role role) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail("user@example.com");
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setRole(role);
    user.setActive(true);
    return user;
  }

  @Test
  void loginSucceedsWithValidCredentials() {
    User user = existingUser("correct-password", Role.TENANT);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    RefreshTokenResult result =
        authService.login(new LoginRequest("user@example.com", "correct-password"));

    assertThat(result.authResponse().accessToken()).isNotBlank();
    assertThat(result.rawRefreshToken()).isNotBlank();
  }

  @Test
  void loginRejectsWrongPassword() {
    User user = existingUser("correct-password", Role.TENANT);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("user@example.com", "wrong-password")))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void loginRejectsUnknownEmail() {
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("nobody@example.com", "whatever123")))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void loginRejectsInactiveUser() {
    User user = existingUser("correct-password", Role.TENANT);
    user.setActive(false);
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("user@example.com", "correct-password")))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void refreshRejectsUnknownToken() {
    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh("some-opaque-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void refreshRejectsBlankToken() {
    assertThatThrownBy(() -> authService.refresh(" "))
        .isInstanceOf(InvalidRefreshTokenException.class);
    assertThatThrownBy(() -> authService.refresh(null))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void refreshRejectsExpiredToken() {
    RefreshToken expired = new RefreshToken();
    expired.setId(UUID.randomUUID());
    expired.setUserId(UUID.randomUUID());
    expired.setExpiresAt(clock.instant().minusSeconds(1));
    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> authService.refresh("expired-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void refreshRejectsAlreadyRevokedToken() {
    RefreshToken revoked = new RefreshToken();
    revoked.setId(UUID.randomUUID());
    revoked.setUserId(UUID.randomUUID());
    revoked.setExpiresAt(clock.instant().plusSeconds(60));
    revoked.setRevokedAt(clock.instant().minusSeconds(1));
    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

    assertThatThrownBy(() -> authService.refresh("reused-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void refreshRotatesValidTokenAndIssuesNewPair() {
    User user = existingUser("correct-password", Role.LANDLORD);
    RefreshToken valid = new RefreshToken();
    valid.setId(UUID.randomUUID());
    valid.setUserId(user.getId());
    valid.setExpiresAt(clock.instant().plusSeconds(60));

    when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(valid));
    when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    RefreshTokenResult result = authService.refresh("valid-token");

    assertThat(valid.getRevokedAt()).isNotNull();
    assertThat(result.authResponse().accessToken()).isNotBlank();
    assertThat(result.rawRefreshToken()).isNotBlank();
  }
}
