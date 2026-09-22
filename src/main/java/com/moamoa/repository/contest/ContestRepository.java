package com.moamoa.repository.contest;

import com.moamoa.domain.contest.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestRepository extends JpaRepository<Contest, Long>, ContestRepositoryCustom {
  Optional<Contest> findBySourceAndSourceId(ContestSource source, String sourceId);

  List<Contest> findByEndDate(LocalDate date);

  @org.springframework.data.jpa.repository.Query(
      "select c from Contest c where c.id<>:id and c.category=:category and c.endDate>=:today order"
          + " by case when c.district=:district then 0 else 1 end,c.endDate,c.id")
  List<Contest> related(
      Long id,
      ContestCategory category,
      String district,
      LocalDate today,
      org.springframework.data.domain.Pageable page);
}
