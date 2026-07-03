package com.darkom.auth.service;

import com.darkom.auth.dto.AuthResponse;
import com.darkom.auth.dto.LoginRequest;
import com.darkom.auth.dto.RegisterRequest;
import com.darkom.auth.dto.UserSummary;
import com.darkom.auth.entity.RefreshToken;
import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.exception.EmailAlreadyInUseException;
import com.darkom.auth.exception.InvalidCredentialsException;
import com.darkom.auth.exception.InvalidRefreshTokenException;
import com.darkom.auth.exception.InvalidRegistrationRoleException;
import com.darkom.auth.repository.RefreshTokenRepository;
import com.darkom.auth.repository.UserRepository;
import com.darkom.common.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      Clock clock) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.clock = clock;
  }

  @Transactional
  public UserSummary register(RegisterRequest request) {
    if (request.role() == Role.ADMIN) {
      throw new InvalidRegistrationRoleException();
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyInUseException(request.email());
    }

    Instant now = clock.instant();
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFullName(request.fullName());
    user.setPhone(request.phone());
    user.setRole(request.role());
    user.setActive(true);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);

    return UserSummary.from(userRepository.save(user));
  }

  @Transactional
  public RefreshTokenResult login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .filter(User::isActive)
            .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    return issueTokenPair(user);
  }

  @Transactional
  public RefreshTokenResult refresh(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw new InvalidRefreshTokenException();
    }

    String hash = hash(rawRefreshToken);
    RefreshToken existing =
        refreshTokenRepository.findByTokenHash(hash).orElseThrow(InvalidRefreshTokenException::new);

    if (!existing.isValid(clock.instant())) {
      throw new InvalidRefreshTokenException();
    }

    existing.setRevokedAt(clock.instant());
    refreshTokenRepository.save(existing);

    User user =
        userRepository
            .findById(existing.getUserId())
            .orElseThrow(InvalidRefreshTokenException::new);

    return issueTokenPair(user);
  }

  private RefreshTokenResult issueTokenPair(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String rawRefreshToken = generateOpaqueToken();

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setId(UUID.randomUUID());
    refreshToken.setUserId(user.getId());
    refreshToken.setTokenHash(hash(rawRefreshToken));
    refreshToken.setExpiresAt(
        clock.instant().plus(jwtService.refreshTokenTtlDays(), ChronoUnit.DAYS));
    refreshToken.setCreatedAt(clock.instant());
    refreshTokenRepository.save(refreshToken);

    AuthResponse authResponse =
        AuthResponse.of(accessToken, jwtService.accessTokenTtlSeconds(), UserSummary.from(user));
    return new RefreshTokenResult(authResponse, rawRefreshToken);
  }

  private String generateOpaqueToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
