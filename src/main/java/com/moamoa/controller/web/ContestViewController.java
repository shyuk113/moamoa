package com.moamoa.controller.web;

import com.moamoa.dto.ContestSearchCondition;
import com.moamoa.security.jwt.CustomUserDetails;
import com.moamoa.service.contest.ContestQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ContestViewController {
  private final ContestQueryService contests;

  public ContestViewController(ContestQueryService contests) {
    this.contests = contests;
  }

  @GetMapping("/")
  public String home(Model model, @AuthenticationPrincipal CustomUserDetails user) {
    var result = contests.search(new ContestSearchCondition(), 0, user == null ? null : user.id());
    model.addAttribute("events", result.page().getContent().stream().limit(6).toList());
    model.addAttribute("favoriteIds", result.favoriteIds());
    return "home";
  }

  @GetMapping("/contests")
  public String list(
      @ModelAttribute("condition") ContestSearchCondition condition,
      @RequestParam(defaultValue = "0") int page,
      @AuthenticationPrincipal CustomUserDetails user,
      Model model) {
    var result = contests.search(condition, page, user == null ? null : user.id());
    model.addAttribute("result", result.page());
    model.addAttribute("events", result.page().getContent());
    model.addAttribute("favoriteIds", result.favoriteIds());
    return "contest/list";
  }

  @GetMapping("/contests/{id}")
  public String detail(
      @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user, Model model) {
    model.addAttribute("event", contests.get(id));
    model.addAttribute("favorite", contests.isFavorite(user == null ? null : user.id(), id));
    return "contest/detail";
  }
}
