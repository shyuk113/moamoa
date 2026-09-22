package com.moamoa.domain.contest;

import com.moamoa.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_reviews")
@Getter
@Setter
@NoArgsConstructor
public class EventReview extends BaseTimeEntity {
  @Version private long version;

  public enum Status {
    PENDING,
    APPROVED,
    HIDDEN
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long contestId;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private int rating;

  @Column(nullable = false, length = 2000)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;
}
