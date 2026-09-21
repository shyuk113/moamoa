package com.moamoa.domain.common;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {
  @Column(nullable = false, updatable = false)
  protected Instant createdAt;

  @Column(nullable = false)
  protected Instant updatedAt;

  @PrePersist
  void create() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void update() {
    updatedAt = Instant.now();
  }
}
