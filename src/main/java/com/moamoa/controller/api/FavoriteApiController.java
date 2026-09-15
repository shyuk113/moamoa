package com.moamoa.controller.api;

import com.moamoa.security.jwt.CustomUserDetails;
import com.moamoa.service.favorite.FavoriteService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteApiController {
  private final FavoriteService favorites;

  public FavoriteApiController(FavoriteService favorites) {
    this.favorites = favorites;
  }

  @PostMapping("/{id}")
  public Map<String, Boolean> add(
      @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
    favorites.add(user.id(), id);
    return Map.of("favorite", true);
  }

  @DeleteMapping("/{id}")
  public Map<String, Boolean> remove(
      @AuthenticationPrincipal CustomUserDetails user, @PathVariable Long id) {
    favorites.remove(user.id(), id);
    return Map.of("favorite", false);
  }
}
