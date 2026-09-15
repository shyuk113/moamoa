package com.moamoa.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class JwtCookieUtil {
  private final boolean secure;

  public JwtCookieUtil(@Value("${app.secure-cookie}") boolean secure) {
    this.secure = secure;
  }

  public String cookie(String token) {
    return ResponseCookie.from("ACCESS_TOKEN", token)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/")
        .maxAge(token.isEmpty() ? 0 : 3600)
        .build()
        .toString();
  }
}
