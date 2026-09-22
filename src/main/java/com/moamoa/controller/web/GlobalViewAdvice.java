package com.moamoa.controller.web;

import com.moamoa.domain.contest.ContestCategory;
import com.moamoa.security.jwt.CustomUserDetails;
import java.time.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalViewAdvice {
  private final Clock clock;

  public GlobalViewAdvice(Clock clock) {
    this.clock = clock;
  }

  @ModelAttribute
  public void common(
      Model model,
      @AuthenticationPrincipal CustomUserDetails user,
      org.springframework.security.core.Authentication authentication,
      java.util.Locale locale) {
    model.addAttribute("currentUser", user);
    model.addAttribute(
        "isAdmin",
        authentication != null
            && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    model.addAttribute("english", "en".equals(locale.getLanguage()));
    model.addAttribute(
        "categories",
        java.util.List.of(
            ContestCategory.CONTEST,
            ContestCategory.ART,
            ContestCategory.FAIR,
            ContestCategory.FESTIVAL));
    model.addAttribute("today", LocalDate.now(clock));
  }
}
