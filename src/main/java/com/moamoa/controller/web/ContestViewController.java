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
  private final com.moamoa.service.contest.EventGuideService guides;
  private final com.moamoa.service.contest.EventReviewService reviews;
  private final com.moamoa.repository.contest.ContestRepository repository;
  private final java.time.Clock clock;

  public ContestViewController(
      ContestQueryService contests,
      com.moamoa.service.contest.EventGuideService guides,
      com.moamoa.service.contest.EventReviewService reviews,
      com.moamoa.repository.contest.ContestRepository repository,
      java.time.Clock clock) {
    this.contests = contests;
    this.guides = guides;
    this.reviews = reviews;
    this.repository = repository;
    this.clock = clock;
  }

  @GetMapping("/")
  public String home(
      Model model, @AuthenticationPrincipal CustomUserDetails user, java.util.Locale locale) {
    var result = contests.search(new ContestSearchCondition(), 0, user == null ? null : user.id());
    model.addAttribute("events", result.page().getContent().stream().limit(6).toList());
    model.addAttribute("favoriteIds", result.favoriteIds());
    model.addAttribute("translatedTitles", guides.titles(result.page().getContent(), locale));
    return "home";
  }

  @GetMapping("/contests")
  public String list(
      @ModelAttribute("condition") ContestSearchCondition condition,
      @RequestParam(defaultValue = "0") int page,
      @AuthenticationPrincipal CustomUserDetails user,
      Model model,
      java.util.Locale locale) {
    var result = contests.search(condition, page, user == null ? null : user.id());
    model.addAttribute("result", result.page());
    model.addAttribute("events", result.page().getContent());
    model.addAttribute("translatedTitles", guides.titles(result.page().getContent(), locale));
    model.addAttribute("favoriteIds", result.favoriteIds());
    return "contest/list";
  }

  @GetMapping("/contests/{id}")
  public String detail(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails user,
      @RequestParam(defaultValue = "0") int reviewPage,
      java.util.Locale locale,
      Model model) {
    var event = contests.get(id);
    model.addAttribute("event", event);
    model.addAttribute("guide", guides.view(event, locale));
    model.addAttribute(
        "reviewOverview", reviews.overview(id, user == null ? null : user.id(), reviewPage));
    var related =
        repository.related(
            id,
            event.getCategory(),
            event.getDistrict(),
            java.time.LocalDate.now(clock),
            org.springframework.data.domain.PageRequest.of(0, 4));
    model.addAttribute("related", related);
    model.addAttribute("relatedTitles", guides.titles(related, locale));
    model.addAttribute("favorite", contests.isFavorite(user == null ? null : user.id(), id));
    return "contest/detail";
  }
}
