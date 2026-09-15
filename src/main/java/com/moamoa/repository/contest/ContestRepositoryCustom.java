package com.moamoa.repository.contest;

import com.moamoa.domain.contest.Contest;
import com.moamoa.dto.ContestSearchCondition;
import org.springframework.data.domain.*;

public interface ContestRepositoryCustom {
  Page<Contest> search(ContestSearchCondition condition, Pageable pageable);
}
