package com.englishplatform.learning.internal.path;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Một lần điều chỉnh lộ trình (FR-018, NFR-PRIV-04): ai sửa, lý do, tóm tắt tự sinh. */
@Entity
@Table(name = "path_revision")
public class PathRevision extends BaseEntity {

  @Column(name = "path_id", nullable = false, updatable = false)
  private UUID pathId;

  @Column(name = "changed_by", nullable = false, updatable = false)
  private UUID changedBy;

  @Column(nullable = false, updatable = false)
  private String reason;

  @Column(nullable = false, updatable = false)
  private String summary;

  protected PathRevision() {}

  PathRevision(UUID pathId, UUID changedBy, String reason, String summary) {
    this.pathId = pathId;
    this.changedBy = changedBy;
    this.reason = reason;
    this.summary = summary;
  }

  public UUID getPathId() {
    return pathId;
  }

  public UUID getChangedBy() {
    return changedBy;
  }

  public String getReason() {
    return reason;
  }

  public String getSummary() {
    return summary;
  }
}
