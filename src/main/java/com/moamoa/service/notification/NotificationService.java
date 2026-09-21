package com.moamoa.service.notification;

import com.moamoa.domain.notification.NotificationType;
import com.moamoa.repository.contest.ContestRepository;
import com.moamoa.repository.favorite.FavoriteRepository;
import com.moamoa.repository.notification.NotificationRepository;
import com.moamoa.repository.user.InterestRepository;
import com.moamoa.service.ingest.ContestCreatedEvent;
import java.time.*;
import java.util.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.*;

@Service
public class NotificationService {
  private final ContestRepository contests;
  private final FavoriteRepository favorites;
  private final InterestRepository interests;
  private final NotificationRepository logs;
  private final NotificationDelivery delivery;
  private final Clock clock;

  public NotificationService(
      ContestRepository contests,
      FavoriteRepository favorites,
      InterestRepository interests,
      NotificationRepository logs,
      NotificationDelivery delivery,
      Clock clock) {
    this.contests = contests;
    this.favorites = favorites;
    this.interests = interests;
    this.logs = logs;
    this.delivery = delivery;
    this.clock = clock;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void created(ContestCreatedEvent event) {
    contests
        .findById(event.contestId())
        .ifPresent(
            c ->
                interests
                    .findUserIds(c.getCategory())
                    .forEach(
                        id ->
                            isolate(
                                () ->
                                    delivery.deliver(
                                        id, c.getId(), NotificationType.NEW_CONTEST))));
  }

  public void deadlines() {
    for (var c : contests.findByEndDate(LocalDate.now(clock).plusDays(3))) {
      var ids = new HashSet<>(interests.findUserIds(c.getCategory()));
      ids.addAll(favorites.findUserIds(c.getId()));
      ids.forEach(id -> isolate(() -> delivery.deliver(id, c.getId(), NotificationType.DEADLINE)));
    }
  }

  public void weekly() {
    logs.findPendingUserIds(NotificationType.PENDING_DIGEST)
        .forEach(id -> isolate(() -> delivery.digest(id)));
  }

  private void isolate(Runnable action) {
    try {
      action.run();
    } catch (RuntimeException ex) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn("Notification recipient failed: {}", ex.getClass().getSimpleName());
    }
  }
}
