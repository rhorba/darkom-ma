package com.darkom.auth;

import com.darkom.auth.dto.AuthResponse;
import com.darkom.auth.dto.LoginRequest;
import com.darkom.auth.dto.RegisterRequest;
import com.darkom.auth.dto.UserSummary;
import com.darkom.auth.service.AuthService;
import com.darkom.auth.service.RefreshTokenResult;
import com.darkom.common.security.CookieProperties;
import com.darkom.common.security.JwtService;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final String REFRESH_COOKIE_NAME = "refresh_token";
  private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

  private final AuthService authService;
  private final JwtService jwtService;
  private final CookieProperties cookieProperties;

  public AuthController(
      AuthService authService, JwtService jwtService, CookieProperties cookieProperties) {
    this.authService = authService;
    this.jwtService = jwtService;
    this.cookieProperties = cookieProperties;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserSummary register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return withRefreshCookie(authService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
    return withRefreshCookie(authService.refresh(refreshToken));
  }

  private ResponseEntity<AuthResponse> withRefreshCookie(RefreshTokenResult result) {
    ResponseCookie cookie =
        ResponseCookie.from(REFRESH_COOKIE_NAME, result.rawRefreshToken())
            .httpOnly(true)
            .secure(cookieProperties.isSecure())
            .sameSite("Strict")
            .path(REFRESH_COOKIE_PATH)
            .maxAge(Duration.ofDays(jwtService.refreshTokenTtlDays()))
            .build();
    return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(result.authResponse());
  }
}
