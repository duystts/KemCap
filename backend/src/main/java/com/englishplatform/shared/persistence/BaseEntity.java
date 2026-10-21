package com.englishplatform.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/** Khóa chính UUID sinh ở ứng dụng + {@code created_at}/{@code updated_at} (UTC). */
@MappedSuperclass
@EntityListeners(EntityTimestampListener.class)
public abstract class BaseEntity implements Persistable<UUID> {

  @Id private UUID id = UUID.randomUUID();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Transient private boolean isNew = true;

  @Override
  public UUID getId() {
    return id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  void touchCreated(Instant now) {
    createdAt = now;
    updatedAt = now;
  }

  void touchUpdated(Instant now) {
    updatedAt = now;
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    isNew = false;
  }
}
