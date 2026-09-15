package com.moamoa.scheduler;

import com.moamoa.service.ingest.ContestIngestService;
import com.moamoa.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class ContestSyncScheduler {
  private final ContestIngestService ingest;
  private final NotificationService notifications;
  private final boolean syncOnStart;

  public ContestSyncScheduler(
      ContestIngestService ingest,
      NotificationService notifications,
      @Value("${app.sync-on-start}") boolean syncOnStart) {
    this.ingest = ingest;
    this.notifications = notifications;
    this.syncOnStart = syncOnStart;
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    if (syncOnStart) ingest.syncAll();
  }

  @Scheduled(cron = "0 0 6,18 * * *", zone = "Asia/Seoul")
  public void sync() {
    ingest.syncAll();
  }

  @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
  public void deadline() {
    notifications.deadlines();
  }

  @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Seoul")
  public void weekly() {
    notifications.weekly();
  }
}
