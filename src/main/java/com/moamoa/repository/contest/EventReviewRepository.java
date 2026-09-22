package com.moamoa.repository.contest;

import com.moamoa.domain.contest.EventReview;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface EventReviewRepository extends JpaRepository<EventReview, Long> {
  interface View {
    Long getId();

    long getVersion();

    Long getContestId();

    int getRating();

    String getBody();

    String getNickname();

    Instant getCreatedAt();

    String getStatus();
  }

  interface Stats {
    long getCount();

    Double getAverage();
  }

  @Query(
      "select r.id as id,r.version as version,r.contestId as contestId,r.rating as rating,r.body as"
          + " body,u.nickname as nickname,r.createdAt as createdAt,r.status as status from"
          + " EventReview r join User u on u.id=r.userId where r.contestId=:id and"
          + " r.status=com.moamoa.domain.contest.EventReview.Status.APPROVED order by r.createdAt"
          + " desc,r.id desc")
  Page<View> published(Long id, Pageable pageable);

  @Query(
      "select count(r) as count,avg(r.rating) as average from EventReview r where r.contestId=:id"
          + " and r.status=com.moamoa.domain.contest.EventReview.Status.APPROVED")
  Stats stats(Long id);

  Optional<EventReview> findByContestIdAndUserId(Long contestId, Long userId);

  @Query("select max(r.updatedAt) from EventReview r where r.userId=:id")
  Instant lastActivity(Long id);

  @Query(
      "select r.id as id,r.version as version,r.contestId as contestId,r.rating as rating,r.body as"
          + " body,u.nickname as nickname,r.createdAt as createdAt,r.status as status from"
          + " EventReview r join User u on u.id=r.userId where r.status=:status order by"
          + " r.createdAt,r.id")
  Page<View> moderation(EventReview.Status status, Pageable pageable);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from EventReview r where r.id=:id")
  Optional<EventReview> lockById(Long id);
}
