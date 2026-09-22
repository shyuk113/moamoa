package com.moamoa.service.contest;

import com.moamoa.domain.contest.EventReview;
import com.moamoa.dto.*;
import com.moamoa.exception.AppException;
import com.moamoa.repository.contest.EventReviewRepository;
import com.moamoa.repository.user.UserRepository;
import java.time.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventReviewService {
  private final EventReviewRepository reviews;
  private final UserRepository users;
  private final ContestQueryService contests;
  private final Clock clock;

  public EventReviewService(
      EventReviewRepository reviews,
      UserRepository users,
      ContestQueryService contests,
      Clock clock) {
    this.reviews = reviews;
    this.users = users;
    this.contests = contests;
    this.clock = clock;
  }

  public record Overview(
      Page<EventReviewRepository.View> page, EventReviewRepository.Stats stats, EventReview own) {}

  public Overview overview(Long event, Long user, int page) {
    return new Overview(
        reviews.published(event, PageRequest.of(Math.max(0, Math.min(page, 10000)), 10)),
        reviews.stats(event),
        user == null ? null : reviews.findByContestIdAndUserId(event, user).orElse(null));
  }

  @Transactional
  public void save(Long event, Long user, int rating, String body) {
    contests.get(event);
    users.lockById(user).orElseThrow();
    if (rating < 1 || rating > 5 || body == null || body.isBlank() || body.length() > 2000)
      throw new AppException(HttpStatus.BAD_REQUEST, "리뷰 입력값을 확인해주세요. / Check your review.");
    Instant last = reviews.lastActivity(user);
    if (last != null && last.isAfter(clock.instant().minusSeconds(60)))
      throw new AppException(
          HttpStatus.TOO_MANY_REQUESTS, "리뷰는 1분 간격으로 작성할 수 있습니다. / Please wait one minute.");
    var r = reviews.findByContestIdAndUserId(event, user).orElse(new EventReview());
    r.setContestId(event);
    r.setUserId(user);
    r.setRating(rating);
    r.setBody(body.trim());
    r.setStatus(EventReview.Status.PENDING);
    reviews.saveAndFlush(r);
  }

  @Transactional
  public void delete(Long id, Long user) {
    users.lockById(user).orElseThrow();
    var r =
        reviews
            .lockById(id)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Review not found"));
    if (!r.getUserId().equals(user))
      throw new AppException(
          HttpStatus.FORBIDDEN, "본인 리뷰만 삭제할 수 있습니다. / Only your own review can be deleted.");
    reviews.delete(r);
  }

  public Page<EventReviewRepository.View> moderation(EventReview.Status status, int page) {
    return reviews.moderation(status, PageRequest.of(Math.max(0, Math.min(page, 10000)), 30));
  }

  @Transactional
  public void moderate(Long id, EventReview.Status status, long version) {
    var r =
        reviews
            .lockById(id)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Review not found"));
    if (r.getVersion() != version)
      throw new AppException(
          HttpStatus.CONFLICT,
          "리뷰가 변경되었습니다. 새로고침 후 다시 검토해주세요. / Review changed. Reload and review it again.");
    r.setStatus(status);
  }
}
