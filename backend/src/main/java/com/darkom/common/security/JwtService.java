package com.darkom.common.security;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

  private static final String ROLE_CLAIM = "role";

  private final JwtProperties properties;
  private final SecretKey signingKey;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(User user) {
    Instant now = Instant.now();
    Instant expiry = now.plus(properties.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim(ROLE_CLAIM, user.getRole().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(signingKey)
        .compact();
  }

  public Claims parseAccessToken(String token) throws JwtException {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  public UUID extractUserId(Claims claims) {
    return UUID.fromString(claims.getSubject());
  }

  public Role extractRole(Claims claims) {
    return Role.valueOf(claims.get(ROLE_CLAIM, String.class));
  }

  public long accessTokenTtlSeconds() {
    return properties.getAccessTokenTtlMinutes() * 60;
  }

  public long refreshTokenTtlDays() {
    return properties.getRefreshTokenTtlDays();
  }
}
