package com.englishplatform.catalog.internal.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/** Hồ sơ có khóa chính = {@code user_account.id} (không sinh id mới như {@code BaseEntity}). */
@MappedSuperclass
abstract class ProfileBase implements Persistable<UUID> {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Transient private boolean isNew = true;

  protected ProfileBase() {}

  protected ProfileBase(UUID userId) {
    this.userId = userId;
  }

  @Override
  public UUID getId() {
    return userId;
  }

  public UUID getUserId() {
    return userId;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PrePersist
  void onPrePersist() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void onPreUpdate() {
    updatedAt = Instant.now();
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    isNew = false;
  }
}
