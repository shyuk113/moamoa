package com.moamoa.client.mapper;

import com.moamoa.domain.contest.Contest;
import java.util.Optional;

public interface ExternalContestMapper<T> {
  Optional<Contest> map(T raw);
}
