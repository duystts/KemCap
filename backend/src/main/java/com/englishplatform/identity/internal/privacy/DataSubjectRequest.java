package com.englishplatform.identity.internal.privacy;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Yêu cầu của chủ thể dữ liệu (FR-031). {@code requester_name} được giữ sau ẩn danh hóa làm bằng
 * chứng tuân thủ (data-model.md).
 */
@Entity
@Table(name = "data_subject_request")
public class DataSubjectRequest extends BaseEntity {

  public enum Type {
    ERASURE,
    ACCESS
  }

  public enum Status {
    RECEIVED,
    COMPLETED,
    REJECTED
  }

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "requester_name", nullable = false)
  private String requesterName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Type type;

  @Column(name = "received_on", nullable = false)
  private LocalDate receivedOn;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.RECEIVED;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolution_note")
  private String resolutionNote;

  @Column(name = "handled_by")
  private UUID handledBy;

  protected DataSubjectRequest() {}

  DataSubjectRequest(UUID studentId, String requesterName, Type type, LocalDate receivedOn) {
    this.studentId = studentId;
    this.requesterName = requesterName;
    this.type = type;
    this.receivedOn = receivedOn;
  }

  boolean isReceived() {
    return status == Status.RECEIVED;
  }

  void complete(UUID by, Instant now) {
    resolve(Status.COMPLETED, null, by, now);
  }

  void reject(String note, UUID by, Instant now) {
    resolve(Status.REJECTED, note, by, now);
  }

  private void resolve(Status target, String note, UUID by, Instant now) {
    this.status = target;
    this.resolutionNote = note;
    this.handledBy = by;
    this.resolvedAt = now;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public String getRequesterName() {
    return requesterName;
  }

  public Type getType() {
    return type;
  }

  public LocalDate getReceivedOn() {
    return receivedOn;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public String getResolutionNote() {
    return resolutionNote;
  }

  public UUID getHandledBy() {
    return handledBy;
  }
}
