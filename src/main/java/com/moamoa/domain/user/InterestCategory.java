package com.moamoa.domain.user;

import com.moamoa.domain.contest.ContestCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "interest_categories",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category"}))
@Getter
@Setter
@NoArgsConstructor
public class InterestCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContestCategory category;

  public InterestCategory(Long userId, ContestCategory category) {
    this.userId = userId;
    this.category = category;
  }
}
