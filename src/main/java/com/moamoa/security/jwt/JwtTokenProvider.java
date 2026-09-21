package com.moamoa.security.jwt;

import com.moamoa.domain.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.Clock;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
  private final SecretKey key;
  private final Clock clock;

  public JwtTokenProvider(@Value("${app.jwt-secret}") String secret, Clock clock) {
    this.clock = clock;
    if (secret.isBlank()) {
      key = Jwts.SIG.HS256.key().build();
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn("JWT_SECRET is unset: temporary signing key; sessions expire on restart.");
    } else {
      if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
        throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
      key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
  }

  public String issue(User user) {
    return Jwts.builder()
        .subject(user.getId().toString())
        .issuer("moamoa")
        .claim("version", user.getTokenVersion())
        .issuedAt(Date.from(clock.instant()))
        .expiration(Date.from(clock.instant().plusSeconds(3600)))
        .signWith(key)
        .compact();
  }

  public Claims verify(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .requireIssuer("moamoa")
        .clock(() -> Date.from(clock.instant()))
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
