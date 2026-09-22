package com.moamoa.service.notification;

import com.moamoa.domain.notification.*;
import com.moamoa.domain.user.*;
import com.moamoa.repository.contest.ContestRepository;
import com.moamoa.repository.notification.NotificationRepository;
import com.moamoa.repository.user.UserRepository;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class NotificationDelivery {
  private final NotificationRepository logs;
  private final UserRepository users;
  private final ContestRepository contests;
  private final EmailSenderService mail;
  private final Clock clock;

  public NotificationDelivery(
      NotificationRepository logs,
      UserRepository users,
      ContestRepository contests,
      EmailSenderService mail,
      Clock clock) {
    this.logs = logs;
    this.users = users;
    this.contests = contests;
    this.mail = mail;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deliver(Long userId, Long contestId, NotificationType type) {
    var contest = contests.findById(contestId).orElseThrow();
    if (contest.isSourceClosed()) return;
    var user = users.findById(userId).orElseThrow();
    var actual =
        type == NotificationType.NEW_CONTEST
                && user.getNotificationFrequency() == NotificationFrequency.WEEKLY
            ? NotificationType.PENDING_DIGEST
            : type;
    if (logs.claim(userId, contestId, actual.name()) == 0) return;
    if (actual == NotificationType.PENDING_DIGEST) return;
    var entry = logs.findByUserIdAndContestIdAndType(userId, contestId, actual).orElseThrow();
    try {
      mail.sendEvents(
          user.getEmail(),
          actual == NotificationType.DEADLINE ? "[moamoa] 마감·종료 3일 전 알림" : "[moamoa] 관심 분야 새 소식",
          List.of(contest));
      entry.setSuccess(true);
      entry.setSentAt(clock.instant());
    } catch (RuntimeException ex) {
      entry.setSuccess(false);
      org.slf4j.LoggerFactory.getLogger(getClass())
          .warn("Notification failed: userId={} contestId={}", userId, contestId);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void digest(Long userId) {
    var u = users.lockById(userId).orElseThrow();
    var pending = logs.findByUserIdAndTypeAndSuccessFalse(userId, NotificationType.PENDING_DIGEST);
    if (pending.isEmpty()) return;
    var events =
        contests.findAllById(pending.stream().map(NotificationLog::getContestId).toList()).stream()
            .filter(c -> !c.isSourceClosed())
            .toList();
    if (!events.isEmpty()) mail.sendEvents(u.getEmail(), "[moamoa] 이번 주 관심 행사 모음", events);
    pending.forEach(
        n -> {
          n.setSuccess(true);
          n.setSentAt(clock.instant());
        });
  }
}
