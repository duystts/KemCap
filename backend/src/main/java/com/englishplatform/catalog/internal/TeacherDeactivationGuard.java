package com.englishplatform.catalog.internal;

import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDeactivationGuard;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Không cho ngừng kích hoạt giáo viên còn ghi danh {@code ACTIVE}/{@code PENDING_PAYMENT} (FR-004).
 */
@Component
class TeacherDeactivationGuard implements UserDeactivationGuard {

  private final EnrollmentRepository enrollments;

  TeacherDeactivationGuard(EnrollmentRepository enrollments) {
    this.enrollments = enrollments;
  }

  @Override
  public void check(UUID userId, Role role) {
    if (role == Role.TEACHER
        && enrollments.existsByTeacherIdAndStatusIn(
            userId, EnumSet.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PENDING_PAYMENT))) {
      throw new BusinessException(ErrorCode.TEACHER_HAS_STUDENTS);
    }
  }
}
