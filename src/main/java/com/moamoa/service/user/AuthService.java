package com.moamoa.service.user;

import com.moamoa.domain.user.User;
import com.moamoa.dto.AuthRequests.*;
import com.moamoa.exception.AppException;
import com.moamoa.repository.user.UserRepository;
import com.moamoa.service.notification.EmailSenderService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final Clock clock;
  private final EmailSenderService mail;
  private final String baseUrl;

  public AuthService(
      UserRepository users,
      PasswordEncoder encoder,
      Clock clock,
      EmailSenderService mail,
      @Value("${app.base-url}") String baseUrl) {
    this.users = users;
    this.encoder = encoder;
    this.clock = clock;
    this.mail = mail;
    this.baseUrl = baseUrl;
  }

  @Transactional
  public User signup(Signup request) {
    var email = request.email().trim().toLowerCase(Locale.ROOT);
    if (users.findByEmail(email).isPresent())
      throw new AppException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
    var u = new User();
    u.setEmail(email);
    u.setPassword(encode(request.password()));
    u.setNickname(request.nickname().trim());
    return users.saveAndFlush(u);
  }

  // Failed attempts must commit even though authentication returns an error.
  @Transactional(noRollbackFor = AppException.class)
  public User login(Login request) {
    var u =
        users
            .lockByEmail(request.email().trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해주세요."));
    var now = clock.instant();
    if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(now)) throw locked(u, now);
    if (u.getLockedUntil() != null) {
      u.setLoginFailCount(0);
      u.setLockedUntil(null);
    }
    if (!encoder.matches(request.password(), u.getPassword())) {
      u.setLoginFailCount(u.getLoginFailCount() + 1);
      if (u.getLoginFailCount() >= 5) {
        u.setLockedUntil(now.plusSeconds(900));
        throw locked(u, now);
      }
      throw new AppException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해주세요.");
    }
    u.setLoginFailCount(0);
    u.setLockedUntil(null);
    return u;
  }

  private AppException locked(User user, Instant now) {
    long mins = Math.max(1, (Duration.between(now, user.getLockedUntil()).getSeconds() + 59) / 60);
    return new AppException(HttpStatus.UNAUTHORIZED, mins + "분 후 다시 시도해주세요.");
  }

  @Transactional
  public void forgot(String email) {
    users
        .lockByEmail(email.trim().toLowerCase(Locale.ROOT))
        .ifPresent(
            u -> {
              if (u.getResetTokenExpiry() != null
                  && u.getResetTokenExpiry().isAfter(clock.instant().plusSeconds(1740))) return;
              byte[] bytes = new byte[32];
              new SecureRandom().nextBytes(bytes);
              String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
              u.setResetToken(hash(raw));
              u.setResetTokenExpiry(clock.instant().plusSeconds(1800));
              mail.sendReset(u.getEmail(), baseUrl + "/auth/reset-password?token=" + raw);
            });
  }

  @Transactional
  public void reset(Reset request) {
    var u = users.lockByResetToken(hash(request.token())).orElseThrow(() -> invalidReset());
    if (u.getResetTokenExpiry() == null || !u.getResetTokenExpiry().isAfter(clock.instant()))
      throw invalidReset();
    u.setPassword(encode(request.password()));
    u.setResetToken(null);
    u.setResetTokenExpiry(null);
    u.setLoginFailCount(0);
    u.setLockedUntil(null);
    u.setTokenVersion(u.getTokenVersion() + 1);
  }

  private String encode(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new AppException(HttpStatus.BAD_REQUEST, "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
    return encoder.encode(password);
  }

  private AppException invalidReset() {
    return new AppException(HttpStatus.BAD_REQUEST, "만료되었거나 이미 사용한 재설정 링크입니다. 다시 요청해주세요.");
  }

  static String hash(String raw) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
