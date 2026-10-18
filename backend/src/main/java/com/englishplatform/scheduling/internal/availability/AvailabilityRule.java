package com.englishplatform.scheduling.internal.availability;

import com.englishplatform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/** Khung rảnh hằng tuần theo múi giờ của giáo viên; {@code day_of_week} 1 = Thứ 2 … 7 = CN. */
@Entity
@Table(name = "availability_rule")
public class AvailabilityRule extends BaseEntity {

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "day_of_week", nullable = false)
  private short dayOfWeek;

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  protected AvailabilityRule() {}

  public AvailabilityRule(UUID teacherId, DayOfWeek day, LocalTime startTime, LocalTime endTime) {
    this.teacherId = teacherId;
    this.dayOfWeek = (short) day.getValue();
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public UUID getTeacherId() {
    return teacherId;
  }

  public DayOfWeek getDayOfWeek() {
    return DayOfWeek.of(dayOfWeek);
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }
}
