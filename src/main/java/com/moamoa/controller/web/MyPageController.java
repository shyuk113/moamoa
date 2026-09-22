package com.moamoa.controller.web;

import com.moamoa.security.jwt.CustomUserDetails;
import com.moamoa.service.contest.ContestQueryService;
import com.moamoa.service.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class MyPageController {
  private final ContestQueryService contests;
  private final UserService users;
  private final com.moamoa.service.contest.EventGuideService guides;

  public MyPageController(
      ContestQueryService contests,
      UserService users,
      com.moamoa.service.contest.EventGuideService guides) {
    this.contests = contests;
    this.users = users;
    this.guides = guides;
  }

  @GetMapping("/mypage/favorites")
  public String favorites(
      @AuthenticationPrincipal CustomUserDetails user, Model model, java.util.Locale locale) {
    var events = contests.favorites(user.id());
    model.addAttribute("events", events);
    model.addAttribute("translatedTitles", guides.titles(events, locale));
    model.addAttribute(
        "favoriteIds",
        events.stream().map(e -> e.getId()).collect(java.util.stream.Collectors.toSet()));
    return "mypage/favorites";
  }

  @GetMapping("/mypage/interests")
  public String interests(@AuthenticationPrincipal CustomUserDetails user, Model model) {
    model.addAttribute("selected", users.interests(user.id()));
    model.addAttribute("frequency", users.frequency(user.id()));
    return "mypage/interests";
  }
}
