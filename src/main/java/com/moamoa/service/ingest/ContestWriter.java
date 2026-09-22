package com.moamoa.service.ingest;

import com.moamoa.domain.contest.Contest;
import com.moamoa.repository.contest.ContestRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContestWriter {
  private final ContestRepository contests;
  private final ApplicationEventPublisher events;
  private final java.time.Clock clock;

  public ContestWriter(
      ContestRepository contests, ApplicationEventPublisher events, java.time.Clock clock) {
    this.contests = contests;
    this.events = events;
    this.clock = clock;
  }

  public enum Result {
    ADDED,
    UPDATED,
    SKIPPED
  }

  @Transactional
  public Result upsert(Contest incoming) {
    var old = contests.findBySourceAndSourceId(incoming.getSource(), incoming.getSourceId());
    if (old.isPresent()) {
      BeanUtils.copyProperties(incoming, old.get(), "id", "createdAt", "updatedAt");
      return Result.UPDATED;
    }
    if (incoming.isSourceClosed() || incoming.getEndDate().isBefore(java.time.LocalDate.now(clock)))
      return Result.SKIPPED;
    var saved = contests.saveAndFlush(incoming);
    events.publishEvent(new ContestCreatedEvent(saved.getId()));
    return Result.ADDED;
  }
}
