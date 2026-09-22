package com.moamoa.domain.contest;

import com.moamoa.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import lombok.*;

@Entity
@Table(
    name = "contests",
    uniqueConstraints = @UniqueConstraint(columnNames = {"source", "source_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Contest extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 1000)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContestType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContestCategory category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContestRegion region;

  private String district;
  private String originalCategory;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  @Column(length = 1000)
  private String place;

  @Column(length = 1000)
  private String eligibility;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OnOfflineType onOffline;

  @Column(length = 2000)
  private String originalUrl;

  @Column(length = 2000)
  private String imageUrl;

  private String fee;

  private Double latitude;
  private Double longitude;
  private String contactPhone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContestSource source;

  @Column(nullable = false, length = 64)
  private String sourceId;

  @Column(nullable = false)
  private boolean sourceClosed;

  public boolean isPermanent() {
    return type != ContestType.CONTEST && LocalDate.of(2099, 12, 31).equals(endDate);
  }

  public String getPeriod() {
    return startDate + " — " + (isPermanent() ? "상설 운영" : endDate);
  }

  public String getDateLabel() {
    return isPermanent() ? "운영 안내" : type == ContestType.CONTEST ? "접수 마감" : "행사 종료";
  }

  public String dayBadge(LocalDate today) {
    if (sourceClosed) return type == ContestType.CONTEST ? "마감" : "종료";
    if (isPermanent()) return "상설";
    long days = ChronoUnit.DAYS.between(today, endDate);
    return days < 0 ? (type == ContestType.CONTEST ? "마감" : "종료") : "D-" + days;
  }
}
