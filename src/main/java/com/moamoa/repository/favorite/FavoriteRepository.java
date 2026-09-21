package com.moamoa.repository.favorite;

import com.moamoa.domain.favorite.Favorite;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
  @Query("select f.contestId from Favorite f where f.userId=:userId and f.contestId in :ids")
  Set<Long> findContestIdsByUserIdAndContestIdIn(Long userId, Collection<Long> ids);

  @Query("select f.contestId from Favorite f where f.userId=:userId order by f.createdAt desc")
  List<Long> findContestIds(Long userId);

  @Query("select f.userId from Favorite f where f.contestId=:contestId")
  List<Long> findUserIds(Long contestId);

  @Modifying
  @Query(
      value =
          "insert into favorites(user_id,contest_id,created_at)"
              + " values(:userId,:contestId,current_timestamp) on conflict(user_id,contest_id) do"
              + " nothing",
      nativeQuery = true)
  int add(Long userId, Long contestId);

  void deleteByUserIdAndContestId(Long userId, Long contestId);
}
