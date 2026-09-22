package com.moamoa.controller.api;

import com.moamoa.security.jwt.CustomUserDetails;
import com.moamoa.service.contest.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class EventCommunityController {
  private final EventReviewService reviews;
  private final EventCalendarService calendars;
  private final ContestQueryService contests;

  public EventCommunityController(
      EventReviewService reviews, EventCalendarService calendars, ContestQueryService contests) {
    this.reviews = reviews;
    this.calendars = calendars;
    this.contests = contests;
  }

  public record ReviewRequest(
      @Min(1) @Max(5) int rating, @NotBlank @Size(max = 2000) String body) {}

  @PutMapping("/api/reviews/events/{id}")
  public Object save(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails user,
      @Valid @RequestBody ReviewRequest r) {
    reviews.save(id, user.id(), r.rating(), r.body());
    return Map.of(
        "message", "리뷰를 저장했습니다. 승인 후 공개됩니다. / Saved. Your review will appear after approval.");
  }

  @DeleteMapping("/api/reviews/{id}")
  public Object delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
    reviews.delete(id, user.id());
    return Map.of("message", "리뷰를 삭제했습니다. / Review deleted.");
  }

  @GetMapping("/contests/{id}/calendar.ics")
  public ResponseEntity<String> calendar(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=moamoa-" + id + ".ics")
        .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
        .body(calendars.calendar(contests.get(id)));
  }
}
