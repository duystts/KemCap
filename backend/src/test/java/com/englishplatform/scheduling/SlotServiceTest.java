package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishplatform.scheduling.internal.SchedulingProperties;
import com.englishplatform.scheduling.internal.availability.AvailabilityException;
import com.englishplatform.scheduling.internal.availability.AvailabilityRule;
import com.englishplatform.scheduling.internal.slot.SlotGenerator;
import com.englishplatform.scheduling.internal.slot.SlotGenerator.Busy;
import com.englishplatform.scheduling.internal.slot.SlotGenerator.Slot;
import com.englishplatform.scheduling.internal.slot.SlotGenerator.Window;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Sinh slot (FR-009, research R4, NFR-UX-02). */
class SlotServiceTest {

  static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
  static final SchedulingProperties PROPS = new SchedulingProperties(12, 2, 30, 10, 15);
  static final UUID TEACHER = UUID.randomUUID();
  // Thứ 2, 02/11/2026 08:00 giờ VN
  static final Instant NOW = Instant.parse("2026-11-02T01:00:00Z");
  static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

  @Test
  void thirtyMinuteStepsWithSessionLength() {
    List<Slot> slots = generate(rule(DayOfWeek.MONDAY, "18:00", "21:00"), 45, List.of(), List.of());
    assertThat(slots)
        .extracting(s -> s.startsAt().atZone(VN).toLocalTime().toString())
        .containsExactly("18:00", "18:30", "19:00", "19:30", "20:00");
    assertThat(slots.getFirst().endsAt())
        .isEqualTo(slots.getFirst().startsAt().plus(Duration.ofMinutes(45)));
    // 18:00 giờ VN = 11:00 UTC
    assertThat(slots.getFirst().startsAt()).isEqualTo(Instant.parse("2026-11-02T11:00:00Z"));
  }

  @Test
  void skipsSlotsBeforeMinimumLeadTime() {
    // bây giờ 16:30 VN → slot sớm nhất 18:30
    Instant now = Instant.parse("2026-11-02T09:30:00Z");
    Window w = SlotGenerator.window(null, null, now, null, PROPS);
    List<Slot> slots =
        SlotGenerator.generate(
            w,
            VN,
            Duration.ofMinutes(60),
            List.of(rule(DayOfWeek.MONDAY, "18:00", "21:00")),
            List.of(),
            List.of());
    assertThat(slots.getFirst().startsAt().atZone(VN).toLocalTime())
        .isEqualTo(LocalTime.of(18, 30));
  }

  @Test
  void windowStopsAtHorizonAndExpiry() {
    Window horizon = SlotGenerator.window(null, NOW.plus(Duration.ofDays(60)), NOW, null, PROPS);
    assertThat(horizon.to()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    Instant expires = NOW.plus(Duration.ofDays(5));
    Window expiry = SlotGenerator.window(null, null, NOW, expires, PROPS);
    assertThat(expiry.to()).isEqualTo(expires);
  }

  @Test
  void fullDayAndPartialExceptionsRemoveSlots() {
    AvailabilityRule monday = rule(DayOfWeek.MONDAY, "18:00", "20:00");
    assertThat(
            generate(
                monday,
                60,
                List.of(new AvailabilityException(TEACHER, MONDAY, null, null, "Nghỉ")),
                List.of()))
        .isEmpty();
    List<Slot> partial =
        generate(
            monday,
            60,
            List.of(
                new AvailabilityException(
                    TEACHER, MONDAY, LocalTime.of(18, 30), LocalTime.of(19, 0), "Họp")),
            List.of());
    // 18:00–19:00 và 18:30–19:30 chạm khoảng nghỉ → chỉ còn 19:00
    assertThat(partial)
        .extracting(s -> s.startsAt().atZone(VN).toLocalTime().toString())
        .containsExactly("19:00");
  }

  @Test
  void busySessionsAreExcluded() {
    Instant busyStart = Instant.parse("2026-11-02T11:30:00Z"); // 18:30 VN
    List<Slot> slots =
        generate(
            rule(DayOfWeek.MONDAY, "18:00", "20:30"),
            45,
            List.of(),
            List.of(new Busy(busyStart, busyStart.plus(Duration.ofMinutes(45)))));
    assertThat(slots)
        .extracting(s -> s.startsAt().atZone(VN).toLocalTime().toString())
        .containsExactly("19:30");
  }

  @Test
  void teacherTimezoneWithDaylightSavingIsConvertedCorrectly() {
    ZoneId berlin = ZoneId.of("Europe/Berlin");
    // CN 29/03/2026: 02:00 → 03:00 (mất một giờ)
    Instant now = Instant.parse("2026-03-26T00:00:00Z");
    Window w = new Window(now, Instant.parse("2026-03-31T00:00:00Z"));
    List<Slot> slots =
        SlotGenerator.generate(
            w,
            berlin,
            Duration.ofMinutes(25),
            List.of(
                rule(DayOfWeek.SUNDAY, "01:30", "03:30"), rule(DayOfWeek.MONDAY, "09:00", "09:30")),
            List.of(),
            List.of());
    assertThat(slots)
        .extracting(Slot::startsAt)
        .containsExactly(
            Instant.parse("2026-03-29T00:30:00Z"), // 01:30 CET
            // 02:00 và 02:30 không tồn tại
            Instant.parse("2026-03-29T01:00:00Z"), // 03:00 CEST
            Instant.parse("2026-03-30T07:00:00Z")); // T2 09:00 CEST = 07:00 UTC
  }

  /** R5 verify U3: ngày tiến giờ, buổi không được kết thúc sau giờ kết thúc lịch rảnh. */
  @Test
  void springForwardSlotsDoNotOverrunRuleEnd() {
    ZoneId berlin = ZoneId.of("Europe/Berlin");
    Window w =
        new Window(Instant.parse("2026-03-28T00:00:00Z"), Instant.parse("2026-03-30T00:00:00Z"));
    List<Slot> slots =
        SlotGenerator.generate(
            w,
            berlin,
            Duration.ofMinutes(60),
            List.of(rule(DayOfWeek.SUNDAY, "01:00", "03:00")),
            List.of(),
            List.of());
    // 01:00 CET + 60 phút = 03:00 CEST (vừa khít); 01:30 → 03:30 CEST vượt giờ kết thúc
    assertThat(slots)
        .extracting(Slot::startsAt)
        .containsExactly(Instant.parse("2026-03-29T00:00:00Z"));
  }

  private static List<Slot> generate(
      AvailabilityRule rule, int minutes, List<AvailabilityException> ex, List<Busy> busy) {
    Window w = SlotGenerator.window(null, NOW.plus(Duration.ofDays(1)), NOW, null, PROPS);
    return SlotGenerator.generate(w, VN, Duration.ofMinutes(minutes), List.of(rule), ex, busy);
  }

  private static AvailabilityRule rule(DayOfWeek day, String start, String end) {
    return new AvailabilityRule(TEACHER, day, LocalTime.parse(start), LocalTime.parse(end));
  }
}
