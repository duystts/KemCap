package com.englishplatform.catalog.internal;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.EnrollmentView;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Stub dùng tạm cho scheduling/learning khi bản thật (U2) chưa có — profile {@code stub}. Mọi ghi
 * danh coi như ACTIVE, còn 10 buổi, và mọi giáo viên đều được phân công.
 */
@Component
@Profile("stub")
class StubEnrollmentApi implements EnrollmentApi {

  @Override
  public EnrollmentView get(UUID enrollmentId) {
    return new EnrollmentView(
        enrollmentId,
        UUID.nameUUIDFromBytes(("student-" + enrollmentId).getBytes()),
        UUID.nameUUIDFromBytes(("teacher-" + enrollmentId).getBytes()),
        12,
        2,
        0,
        45,
        Instant.now().plus(90, ChronoUnit.DAYS),
        EnrollmentStatus.ACTIVE);
  }

  @Override
  public void reserve(UUID enrollmentId) {}

  @Override
  public void release(UUID enrollmentId) {}

  @Override
  public void consume(UUID enrollmentId) {}

  @Override
  public void consumeReleased(UUID enrollmentId) {}

  @Override
  public boolean isAssigned(UUID teacherId, UUID studentId) {
    return true;
  }
}
