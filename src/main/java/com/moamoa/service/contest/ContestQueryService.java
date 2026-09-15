package com.moamoa.service.contest;

import com.moamoa.domain.contest.Contest;
import com.moamoa.dto.ContestSearchCondition;
import com.moamoa.exception.AppException;
import com.moamoa.repository.contest.ContestRepository;
import com.moamoa.repository.favorite.FavoriteRepository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContestQueryService {
  private final ContestRepository contests;
  private final FavoriteRepository favorites;

  public ContestQueryService(ContestRepository contests, FavoriteRepository favorites) {
    this.contests = contests;
    this.favorites = favorites;
  }

  public record SearchResult(Page<Contest> page, Set<Long> favoriteIds) {}

  public SearchResult search(ContestSearchCondition c, int page, Long userId) {
    if (c.getFrom() != null && c.getTo() != null && c.getFrom().isAfter(c.getTo()))
      throw new AppException(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
    var result = contests.search(c, PageRequest.of(Math.max(0, Math.min(page, 10000)), 12));
    var ids = result.getContent().stream().map(Contest::getId).toList();
    return new SearchResult(
        result,
        userId == null || ids.isEmpty()
            ? Set.of()
            : favorites.findContestIdsByUserIdAndContestIdIn(userId, ids));
  }

  public Contest get(Long id) {
    return contests
        .findById(id)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));
  }

  public boolean isFavorite(Long userId, Long id) {
    return userId != null
        && favorites.findContestIdsByUserIdAndContestIdIn(userId, List.of(id)).contains(id);
  }

  public List<Contest> favorites(Long userId) {
    var ids = favorites.findContestIds(userId);
    var found = contests.findAllById(ids);
    var order = new HashMap<Long, Integer>();
    for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
    found.sort(Comparator.comparingInt(c -> order.get(c.getId())));
    return found;
  }
}
