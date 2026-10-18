package com.englishplatform.notification.internal;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Thông báo trong hệ thống (chuông thông báo). */
@Entity
@Table(name = "notification")
public class Notification extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String title;

  private String body;

  private String link;

  @Column(name = "read_at")
  private Instant readAt;

  protected Notification() {}

  public Notification(UUID userId, String type, String title, String body, String link) {
    this.userId = userId;
    this.type = type;
    this.title = title;
    this.body = body;
    this.link = link;
  }

  public void markRead(Instant at) {
    if (readAt == null) {
      readAt = at;
    }
  }

  public UUID getUserId() {
    return userId;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getLink() {
    return link;
  }

  public Instant getReadAt() {
    return readAt;
  }
}
