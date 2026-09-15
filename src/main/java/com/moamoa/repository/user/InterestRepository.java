package com.moamoa.repository.user;

import com.moamoa.domain.contest.ContestCategory;
import com.moamoa.domain.user.InterestCategory;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface InterestRepository extends JpaRepository<InterestCategory, Long> {
  List<InterestCategory> findByUserId(Long userId);

  void deleteByUserId(Long userId);

  @Query("select i.userId from InterestCategory i where i.category=:category")
  List<Long> findUserIds(ContestCategory category);
}
