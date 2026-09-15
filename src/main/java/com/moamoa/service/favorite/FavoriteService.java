package com.moamoa.service.favorite;

import com.moamoa.repository.favorite.FavoriteRepository;
import com.moamoa.service.contest.ContestQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {
  private final FavoriteRepository favorites;
  private final ContestQueryService contests;

  public FavoriteService(FavoriteRepository favorites, ContestQueryService contests) {
    this.favorites = favorites;
    this.contests = contests;
  }

  @Transactional
  public void add(Long userId, Long contestId) {
    contests.get(contestId);
    favorites.add(userId, contestId);
  }

  @Transactional
  public void remove(Long userId, Long contestId) {
    favorites.deleteByUserIdAndContestId(userId, contestId);
  }
}
