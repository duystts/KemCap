package com.englishplatform.scheduling.internal.slot;

import com.englishplatform.scheduling.internal.SchedulingProperties;
import com.englishplatform.scheduling.internal.availability.AvailabilityException;
import com.englishplatform.scheduling.internal.availability.AvailabilityRule;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sinh khung giờ trống (research R4), thuần logic để unit test: bước 30 phút trong từng khung rảnh
 * theo múi giờ giáo viên, độ dài = thời lượng buổi, bỏ slot trước {@code now + lead}, sau {@code
 * now + horizon} hoặc sau hạn gói, trừ ngày nghỉ và khoảng bận. Giờ không tồn tại khi chuyển DST bị
 * bỏ.
 */
public final class SlotGenerator {

  private SlotGenerator() {}

  public record Slot(Instant startsAt, Instant endsAt) {}

  public record Busy(Instant startsAt, Instant endsAt) {}

  public record Window(Instant from, Instant to) {}

  /** Cửa sổ thực tế: giao của yêu cầu với [now + lead, min(now + horizon, expiresAt)]. */
  public static Window window(
      Instant from, Instant to, Instant now, Instant expiresAt, SchedulingProperties props) {
    Instant earliest = now.plus(props.minBookingLead());
    Instant latest = now.plus(props.bookingHorizon());
    if (expiresAt != null && expiresAt.isBefore(latest)) {
      latest = expiresAt;
    }
    Instant start = from == null || from.isBefore(earliest) ? earliest : from;
    Instant end = to == null || to.isAfter(latest) ? latest : to;
    return new Window(start, end);
  }

  public static List<Slot> generate(
      Window window,
      ZoneId zone,
      Duration length,
      List<AvailabilityRule> rules,
      List<AvailabilityException> exceptions,
      List<Busy> busy) {
    List<Slot> slots = new ArrayList<>();
    if (!window.from().isBefore(window.to())) {
      return slots;
    }
    LocalDate day = window.from().atZone(zone).toLocalDate();
    LocalDate lastDay = window.to().atZone(zone).toLocalDate();
    for (; !day.isAfter(lastDay); day = day.plusDays(1)) {
      LocalDate date = day;
      if (exceptions.stream().anyMatch(e -> e.getDate().equals(date) && e.isFullDay())) {
        continue;
      }
      for (AvailabilityRule rule : rules) {
        if (rule.getDayOfWeek() != date.getDayOfWeek()) {
          continue;
        }
        int ruleEnd = rule.getEndTime().toSecondOfDay() / 60;
        int lengthMinutes = (int) length.toMinutes();
        int step = (int) SchedulingProperties.SLOT_STEP.toMinutes();
        for (int minute = rule.getStartTime().toSecondOfDay() / 60;
            minute + lengthMinutes <= ruleEnd;
            minute += step) {
          LocalTime start = LocalTime.ofSecondOfDay(minute * 60L);
          LocalTime end = LocalTime.ofSecondOfDay((minute + lengthMinutes) * 60L % 86_400);
          if (inException(exceptions, date, start, end)) {
            continue;
          }
          LocalDateTime local = LocalDateTime.of(date, start);
          if (!zone.getRules().isValidOffset(local, zone.getRules().getOffset(local))) {
            continue; // giờ rơi vào khoảng nhảy DST
          }
          Instant startsAt = ZonedDateTime.of(local, zone).toInstant();
          Instant endsAt = startsAt.plus(length);
          ZonedDateTime localEnd = endsAt.atZone(zone);
          if (!localEnd.toLocalDate().equals(date)
              || localEnd.toLocalTime().isAfter(rule.getEndTime())) {
            continue; // ngày tiến giờ DST: buổi dài hơn trên đồng hồ treo tường
          }
          if (startsAt.isBefore(window.from()) || endsAt.isAfter(window.to())) {
            continue;
          }
          boolean taken =
              busy.stream()
                  .anyMatch(b -> b.startsAt().isBefore(endsAt) && b.endsAt().isAfter(startsAt));
          if (!taken) {
            slots.add(new Slot(startsAt, endsAt));
          }
        }
      }
    }
    slots.sort((a, b) -> a.startsAt().compareTo(b.startsAt()));
    return slots;
  }

  private static boolean inException(
      List<AvailabilityException> exceptions, LocalDate date, LocalTime start, LocalTime end) {
    return exceptions.stream()
        .filter(e -> e.getDate().equals(date) && !e.isFullDay())
        .anyMatch(e -> e.getStartTime().isBefore(end) && e.getEndTime().isAfter(start));
  }
}
