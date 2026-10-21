package com.englishplatform.shared.persistence;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Clock;
import java.time.Instant;

/**
 * Ghi {@code created_at}/{@code updated_at} theo {@link Clock} của app (R18) để test điều khiển
 * được thời gian. Hibernate lấy instance qua Spring ({@code SpringBeanContainer}) nên Clock được
 * tiêm.
 */
public class EntityTimestampListener {

  private final Clock clock;

  public EntityTimestampListener(Clock clock) {
    this.clock = clock;
  }

  @PrePersist
  void onPrePersist(BaseEntity entity) {
    entity.touchCreated(Instant.now(clock));
  }

  @PreUpdate
  void onPreUpdate(BaseEntity entity) {
    entity.touchUpdated(Instant.now(clock));
  }
}
