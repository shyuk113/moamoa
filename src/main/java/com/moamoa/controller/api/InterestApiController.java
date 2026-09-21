package com.moamoa.controller.api;

import com.moamoa.domain.contest.ContestCategory;
import com.moamoa.domain.user.NotificationFrequency;
import com.moamoa.security.jwt.CustomUserDetails;
import com.moamoa.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interests")
public class InterestApiController {
  private final UserService users;

  public InterestApiController(UserService users) {
    this.users = users;
  }

  public record Request(
      @NotNull @Size(max = 5) Set<@NotNull ContestCategory> categories,
      @NotNull NotificationFrequency frequency) {}

  @PutMapping
  public Map<String, String> update(
      @AuthenticationPrincipal CustomUserDetails user, @Valid @RequestBody Request request) {
    users.update(user.id(), request.categories(), request.frequency());
    return Map.of("message", "관심분야와 알림 설정을 저장했습니다.");
  }
}
