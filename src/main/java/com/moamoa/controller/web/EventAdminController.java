package com.moamoa.controller.web;

import com.moamoa.domain.contest.EventReview;
import com.moamoa.dto.EventGuideRequest;
import com.moamoa.security.jwt.CustomUserDetails;
import com.moamoa.service.contest.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class EventAdminController {
  private final EventGuideService guides;
  private final ContestQueryService contests;
  private final EventReviewService reviews;

  public EventAdminController(
      EventGuideService guides, ContestQueryService contests, EventReviewService reviews) {
    this.guides = guides;
    this.contests = contests;
    this.reviews = reviews;
  }

  @GetMapping("/admin/contests/{id}/guide")
  public String edit(
      @PathVariable Long id, @RequestParam(defaultValue = "ko") String language, Model model) {
    model.addAttribute("event", contests.get(id));
    model.addAttribute("guide", guides.editable(id, language));
    return "admin/guide";
  }

  @PutMapping("/api/admin/contests/{id}/guide/{language}")
  @ResponseBody
  public Object save(
      @PathVariable Long id,
      @PathVariable String language,
      @Valid @RequestBody EventGuideRequest r,
      @AuthenticationPrincipal CustomUserDetails user) {
    guides.save(id, language, r, user.id());
    return Map.of("message", "방문 안내를 저장했습니다. / Guide saved.");
  }

  @GetMapping("/admin/reviews")
  public String queue(
      @RequestParam(defaultValue = "PENDING") EventReview.Status status,
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    model.addAttribute("reviews", reviews.moderation(status, page));
    model.addAttribute("reviewStatus", status);
    return "admin/reviews";
  }

  public record Moderation(@NotNull EventReview.Status status, @NotNull Long version) {}

  @PutMapping("/api/admin/reviews/{id}")
  @ResponseBody
  public Object moderate(@PathVariable Long id, @Valid @RequestBody Moderation r) {
    reviews.moderate(id, r.status(), r.version());
    return Map.of("message", "리뷰 공개 상태를 변경했습니다. / Review status updated.");
  }
}
