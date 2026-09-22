package com.moamoa.repository.contest;

import com.moamoa.domain.contest.EventGuide;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventGuideRepository extends JpaRepository<EventGuide, Long> {
  Optional<EventGuide> findByContestIdAndLanguage(Long contestId, String language);

  List<EventGuide> findByContestIdInAndLanguage(Collection<Long> ids, String language);
}
