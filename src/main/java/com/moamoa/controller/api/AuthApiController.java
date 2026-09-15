package com.moamoa.controller.api;

import com.moamoa.dto.AuthRequests.*;
import com.moamoa.security.jwt.*;
import com.moamoa.service.user.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
  private final AuthService auth;
  private final JwtTokenProvider tokens;
  private final JwtCookieUtil cookies;

  public AuthApiController(AuthService auth, JwtTokenProvider tokens, JwtCookieUtil cookies) {
    this.auth = auth;
    this.tokens = tokens;
    this.cookies = cookies;
  }

  @PostMapping("/signup")
  public ResponseEntity<?> signup(@Valid @RequestBody Signup request) {
    auth.signup(request);
    return ResponseEntity.status(201).body(Map.of("message", "회원가입이 완료되었습니다."));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody Login request) {
    var user = auth.login(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookies.cookie(tokens.issue(user)))
        .body(Map.of("message", "로그인되었습니다."));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout() {
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookies.cookie(""))
        .body(Map.of("message", "로그아웃되었습니다."));
  }

  @PostMapping("/forgot-password")
  public Map<String, String> forgot(@Valid @RequestBody Forgot request) {
    auth.forgot(request.email());
    return Map.of("message", "가입된 이메일이면 재설정 링크를 보냈습니다. 메일함을 확인해주세요.");
  }

  @PostMapping("/reset-password")
  public Map<String, String> reset(@Valid @RequestBody Reset request) {
    auth.reset(request);
    return Map.of("message", "비밀번호를 변경했습니다. 새 비밀번호로 로그인해주세요.");
  }
}
