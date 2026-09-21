package com.moamoa.domain.notification;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(
    name = "notification_logs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "contest_id", "type"}))
@Getter
@Setter
@NoArgsConstructor
public class NotificationLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long contestId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType type;

  private Instant sentAt;

  @Column(nullable = false)
  private boolean success;
}
