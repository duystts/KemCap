package com.englishplatform.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.englishplatform.catalog.internal.enrollment.Enrollment;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Bộ đếm buổi và máy trạng thái ghi danh (data-model.md, FR-024, SC-007). */
class EnrollmentTest {

  static final Instant NOW = Instant.parse("2026-11-01T03:00:00Z");

  @Test
  void pendingPaymentCannotReserve() {
    Enrollment e = enrollment(2);
    assertCode(() -> e.reserve(NOW), ErrorCode.ENROLLMENT_NOT_ACTIVE);
  }

  @Test
  void activateSetsExpiryFromValidityDays() {
    Enrollment e = enrollment(2);
    e.activate(NOW);
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(e.getActivatedAt()).isEqualTo(NOW);
    assertThat(e.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(90)));
    assertCode(() -> e.activate(NOW), ErrorCode.INVALID_STATE_TRANSITION);
  }

  @Test
  void reserveUntilNoSessionsLeft() {
    Enrollment e = active(2);
    e.reserve(NOW);
    e.reserve(NOW);
    assertThat(e.sessionsRemaining()).isZero();
    assertCode(() -> e.reserve(NOW), ErrorCode.NO_SESSIONS_LEFT);
  }

  @Test
  void expiredPackageCannotReserve() {
    Enrollment e = active(2);
    assertCode(() -> e.reserve(NOW.plus(Duration.ofDays(90))), ErrorCode.ENROLLMENT_NOT_ACTIVE);
  }

  @Test
  void consumeMovesReservedToConsumedAndExhausts() {
    Enrollment e = active(2);
    e.reserve(NOW);
    e.reserve(NOW);
    e.consume();
    assertThat(e.getSessionsReserved()).isEqualTo(1);
    assertThat(e.getSessionsConsumed()).isEqualTo(1);
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    e.consume();
    assertThat(e.getSessionsConsumed()).isEqualTo(2);
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.EXHAUSTED);
  }

  @Test
  void consumeOrReleaseWithoutReservationIsInvalid() {
    Enrollment e = active(2);
    assertCode(e::consume, ErrorCode.INVALID_STATE_TRANSITION);
    assertCode(e::release, ErrorCode.INVALID_STATE_TRANSITION);
  }

  @Test
  void releaseGivesSessionBack() {
    Enrollment e = active(1);
    e.reserve(NOW);
    e.release();
    assertThat(e.getSessionsReserved()).isZero();
    assertThat(e.sessionsRemaining()).isEqualTo(1);
  }

  @Test
  void consumeReleasedNeedsAFreeSession() {
    Enrollment e = active(1);
    e.reserve(NOW);
    e.release();
    e.consumeReleased();
    assertThat(e.getSessionsConsumed()).isEqualTo(1);
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.EXHAUSTED);
    assertCode(e::consumeReleased, ErrorCode.NO_SESSIONS_LEFT);
  }

  @Test
  void extendReactivatesExpiredEnrollment() {
    Enrollment e = active(2);
    e.expire();
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.EXPIRED);
    UUID admin = UUID.randomUUID();
    Instant later = NOW.plus(Duration.ofDays(120));
    e.extend(later, admin, NOW.plus(Duration.ofDays(95)));
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(e.getExpiresAt()).isEqualTo(later);
    assertThat(e.getExtendedBy()).isEqualTo(admin);
  }

  @Test
  void extendMustMoveExpiryIntoTheFuture() {
    Enrollment e = active(2);
    assertCode(
        () -> e.extend(NOW.minusSeconds(1), UUID.randomUUID(), NOW), ErrorCode.VALIDATION_FAILED);
  }

  @Test
  void cancelOnlyFromPendingOrActiveWithoutBookedSessions() {
    Enrollment pending = enrollment(2);
    pending.cancel();
    assertThat(pending.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    assertCode(pending::cancel, ErrorCode.INVALID_STATE_TRANSITION);

    Enrollment booked = active(2);
    booked.reserve(NOW);
    assertCode(booked::cancel, ErrorCode.INVALID_STATE_TRANSITION);
  }

  @Test
  void changeTeacherRejectsSameTeacher() {
    Enrollment e = active(2);
    assertCode(() -> e.changeTeacher(e.getTeacherId()), ErrorCode.VALIDATION_FAILED);
    UUID other = UUID.randomUUID();
    e.changeTeacher(other);
    assertThat(e.getTeacherId()).isEqualTo(other);
  }

  /** R7 verify U3: trả lại buổi trên gói đã quá hạn không đưa gói về ACTIVE. */
  @Test
  void unconsumeOnExpiredPackageStaysExpired() {
    Enrollment e = active(1);
    e.reserve(NOW);
    e.consume();
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.EXHAUSTED);
    e.unconsume(NOW.plus(Duration.ofDays(91)));
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.EXPIRED);

    Enrollment fresh = active(1);
    fresh.reserve(NOW);
    fresh.consume();
    fresh.unconsume(NOW.plus(Duration.ofDays(1)));
    assertThat(fresh.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    assertThat(fresh.sessionsRemaining()).isEqualTo(1);
  }

  private static Enrollment enrollment(int sessions) {
    return new Enrollment(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), sessions, 45, 90, 1_200_000L);
  }

  private static Enrollment active(int sessions) {
    Enrollment e = enrollment(sessions);
    e.activate(NOW);
    return e;
  }

  private static void assertCode(Runnable action, ErrorCode code) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).code())
        .isEqualTo(code);
  }
}
