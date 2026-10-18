package com.englishplatform.scheduling.internal.availability;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Ngày nghỉ của giáo viên: cả ngày (giờ null) hoặc một khoảng giờ, theo múi giờ giáo viên. */
@Entity
@Table(name = "availability_exception")
public class AvailabilityException extends BaseEntity {

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(nullable = false)
  private LocalDate date;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  private String reason;

  protected AvailabilityException() {}

  public AvailabilityException(
      UUID teacherId, LocalDate date, LocalTime startTime, LocalTime endTime, String reason) {
    this.teacherId = teacherId;
    this.date = date;
    this.startTime = startTime;
    this.endTime = endTime;
    this.reason = reason;
  }

  public boolean isFullDay() {
    return startTime == null;
  }

  public UUID getTeacherId() {
    return teacherId;
  }

  public LocalDate getDate() {
    return date;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public String getReason() {
    return reason;
  }
}
