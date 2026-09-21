package com.moamoa.controller.api;

import com.moamoa.dto.ContestSearchCondition;
import com.moamoa.security.jwt.CustomUserDetails;
import com.moamoa.service.contest.ContestQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contests")
public class ContestApiController {
  private final ContestQueryService contests;

  public ContestApiController(ContestQueryService contests) {
    this.contests = contests;
  }

  @GetMapping
  public Object list(
      @ModelAttribute ContestSearchCondition condition,
      @RequestParam(defaultValue = "0") int page,
      @AuthenticationPrincipal CustomUserDetails user) {
    var result = contests.search(condition, page, user == null ? null : user.id());
    return java.util.Map.of(
        "items",
        result.page().getContent(),
        "page",
        result.page().getNumber(),
        "totalPages",
        result.page().getTotalPages(),
        "totalElements",
        result.page().getTotalElements(),
        "favoriteIds",
        result.favoriteIds());
  }

  @GetMapping("/{id}")
  public Object detail(@PathVariable Long id) {
    return contests.get(id);
  }
}
