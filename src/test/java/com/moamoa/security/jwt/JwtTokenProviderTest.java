package com.moamoa.security.jwt;

import static org.assertj.core.api.Assertions.*;

import com.moamoa.domain.user.User;
import io.jsonwebtoken.JwtException;
import java.time.*;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {
  private final String secret = "a".repeat(64);
  private final Clock now = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void validatesSubjectAndRejectsTamperedOrExpiredToken() {
    var provider = new JwtTokenProvider(secret, now);
    var u = new User();
    u.setId(42L);
    String token = provider.issue(u);
    assertThat(provider.verify(token).getSubject()).isEqualTo("42");
    assertThatThrownBy(() -> new JwtTokenProvider("b".repeat(64), now).verify(token))
        .isInstanceOf(JwtException.class);
    assertThatThrownBy(
            () ->
                new JwtTokenProvider(secret, Clock.offset(now, Duration.ofHours(2))).verify(token))
        .isInstanceOf(JwtException.class);
  }
}
