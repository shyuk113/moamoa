package com.moamoa.domain.user;

import com.moamoa.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false, length = 40)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.USER;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationFrequency notificationFrequency = NotificationFrequency.IMMEDIATE;

  @Column(unique = true, length = 64)
  private String resetToken;

  private Instant resetTokenExpiry;

  @Column(nullable = false)
  private int loginFailCount;

  private Instant lockedUntil;

  @Column(nullable = false)
  private int tokenVersion;
}
