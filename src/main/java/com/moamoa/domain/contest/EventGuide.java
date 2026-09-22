package com.moamoa.domain.contest;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "event_guides")
@Getter
@Setter
@NoArgsConstructor
public class EventGuide {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long contestId;

  @Column(nullable = false, length = 10)
  private String language;

  @Column(length = 1000)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(length = 1000)
  private String place;

  @Column(length = 1000)
  private String fee;

  @Column(length = 1000)
  private String eligibility;

  @Column(columnDefinition = "text")
  private String transit;

  @Column(columnDefinition = "text")
  private String parking;

  @Column(columnDefinition = "text")
  private String booking;

  @Column(columnDefinition = "text")
  private String accessibility;

  private String contactPhone;

  @Column(length = 254)
  private String contactEmail;

  @Column(length = 1000)
  private String address;

  private Double latitude;
  private Double longitude;

  @Column(columnDefinition = "text")
  private String gallery;

  @Column(length = 11)
  private String videoId;

  @Column(length = 1000)
  private String attribution;

  @Column(length = 2000)
  private String sourceUrl;

  @Column(nullable = false)
  private Long updatedBy;

  @Column(nullable = false)
  private Instant updatedAt;
}
