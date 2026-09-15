package com.moamoa.client;

import com.moamoa.domain.contest.*;
import java.util.List;

public interface ExternalContestClient {
  ContestSource source();

  boolean enabled();

  FetchResult fetch();

  record FetchResult(List<Contest> contests, int scanned, int skipped, int invalid, int fallback) {}
}
