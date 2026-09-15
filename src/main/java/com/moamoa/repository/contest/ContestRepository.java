package com.moamoa.repository.contest;

import com.moamoa.domain.contest.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestRepository extends JpaRepository<Contest, Long>, ContestRepositoryCustom {
  Optional<Contest> findBySourceAndSourceId(ContestSource source, String sourceId);

  List<Contest> findByEndDate(LocalDate date);
}
