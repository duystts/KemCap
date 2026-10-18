package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.englishplatform.scheduling.internal.session.CounterAction;
import com.englishplatform.scheduling.internal.session.Session;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Bảng chuyển trạng thái buổi học và ảnh hưởng tới số buổi (data-model.md, research R5). */
class SessionOutcomeTest {

  static final Instant START = Instant.parse("2026-11-03T12:00:00Z");
  static final Duration CUTOFF = Duration.ofHours(12);
  static final UUID ADMIN = UUID.randomUUID();

  @ParameterizedTest
  @CsvSource({
    "COMPLETED, CONSUME",
    "STUDENT_NO_SHOW, CONSUME",
    "INTERRUPTED, RELEASE",
    "TEACHER_ABSENT, RELEASE"
  })
  void outcomeChargesOrRefunds(SessionStatus outcome, CounterAction expected) {
    Session s = session();
    assertThat(s.recordOutcome(outcome, s.getTeacherId(), START.plusSeconds(60)))
        .isEqualTo(expected);
    assertThat(s.getStatus()).isEqualTo(outcome);
    assertThat(s.getOutcomeBy()).isEqualTo(s.getTeacherId());
  }

  @Test
  void outcomeBeforeStartIsInvalid() {
    Session s = session();
    assertCode(
        () -> s.recordOutcome(SessionStatus.COMPLETED, s.getTeacherId(), START.minusSeconds(1)),
        ErrorCode.INVALID_STATE_TRANSITION);
  }

  @Test
  void outcomeMustBeAFinalOutcome() {
    Session s = session();
    assertCode(
        () -> s.recordOutcome(SessionStatus.CANCELLED_LATE, s.getTeacherId(), START.plusSeconds(1)),
        ErrorCode.VALIDATION_FAILED);
  }

  @Test
  void cancelOnTimeRefundsAndLateCharges() {
    Session onTime = session();
    assertThat(onTime.cancel(START.minus(CUTOFF), CUTOFF, ADMIN, "Bận"))
        .isEqualTo(CounterAction.RELEASE);
    assertThat(onTime.getStatus()).isEqualTo(SessionStatus.CANCELLED_ON_TIME);

    Session late = session();
    assertThat(late.cancel(START.minus(CUTOFF).plusSeconds(1), CUTOFF, ADMIN, null))
        .isEqualTo(CounterAction.CONSUME);
    assertThat(late.getStatus()).isEqualTo(SessionStatus.CANCELLED_LATE);
  }

  @Test
  void cancelAfterStartIsInvalid() {
    Session s = session();
    assertCode(() -> s.cancel(START, CUTOFF, ADMIN, null), ErrorCode.INVALID_STATE_TRANSITION);
  }

  @Test
  void teacherCancelMeansTeacherAbsentAndRefunds() {
    Session s = session();
    assertThat(s.cancelByTeacher(s.getTeacherId(), "Ốm")).isEqualTo(CounterAction.RELEASE);
    assertThat(s.getStatus()).isEqualTo(SessionStatus.TEACHER_ABSENT);
  }

  @Test
  void systemCancelRefundsOnlyFutureBookedSessions() {
    Session s = session();
    assertThat(s.cancelBySystem(START.minusSeconds(1))).contains(CounterAction.RELEASE);
    assertThat(s.getStatus()).isEqualTo(SessionStatus.CANCELLED_SYSTEM);
    // phát lại event: không làm gì
    assertThat(s.cancelBySystem(START.minusSeconds(1))).isEmpty();
    Session started = session();
    assertThat(started.cancelBySystem(START)).isEmpty();
    assertThat(started.getStatus()).isEqualTo(SessionStatus.BOOKED);
  }

  @Test
  void interruptionCountBecomesCompletedAndMakeUpKeepsInterrupted() {
    Session s = session();
    s.recordOutcome(SessionStatus.INTERRUPTED, s.getTeacherId(), START.plusSeconds(600));
    assertThat(s.decideInterruption(false, ADMIN, START)).isEmpty();
    assertThat(s.getStatus()).isEqualTo(SessionStatus.INTERRUPTED);
    assertThat(s.decideInterruption(true, ADMIN, START)).contains(CounterAction.CONSUME_RELEASED);
    assertThat(s.getOutcomeAt()).isEqualTo(START);
    assertThat(s.getStatus()).isEqualTo(SessionStatus.COMPLETED);
    assertCode(() -> s.decideInterruption(true, ADMIN, START), ErrorCode.INVALID_STATE_TRANSITION);
  }

  @Test
  void adminCorrectionWithinSevenDaysRecalculatesCounters() {
    Session s = session();
    Instant end = s.getEndsAt();
    s.recordOutcome(SessionStatus.COMPLETED, s.getTeacherId(), end);
    assertThat(s.correctOutcome(SessionStatus.TEACHER_ABSENT, ADMIN, end.plus(Duration.ofDays(7))))
        .contains(CounterAction.UNCONSUME);
    assertThat(s.correctOutcome(SessionStatus.STUDENT_NO_SHOW, ADMIN, end.plus(Duration.ofDays(1))))
        .contains(CounterAction.CONSUME_RELEASED);
    assertThat(s.correctOutcome(SessionStatus.COMPLETED, ADMIN, end.plus(Duration.ofDays(1))))
        .isEmpty();
    assertCode(
        () -> s.correctOutcome(SessionStatus.TEACHER_ABSENT, ADMIN, end.plus(Duration.ofDays(8))),
        ErrorCode.INVALID_STATE_TRANSITION);
  }

  /** R1 verify U3: buổi giáo viên hủy trước giờ học không "sửa kết quả" thành đã học được. */
  @Test
  void teacherCancelledSessionCannotBeCorrectedIntoAnOutcome() {
    Session s = session();
    s.cancelByTeacher(s.getTeacherId(), "Ốm");
    assertCode(
        () -> s.correctOutcome(SessionStatus.COMPLETED, ADMIN, START.plusSeconds(3600)),
        ErrorCode.INVALID_STATE_TRANSITION);
  }

  private static Session session() {
    return new Session(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        START,
        45,
        UUID.randomUUID(),
        null);
  }

  private static void assertCode(Runnable action, ErrorCode code) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).code())
        .isEqualTo(code);
  }
}
