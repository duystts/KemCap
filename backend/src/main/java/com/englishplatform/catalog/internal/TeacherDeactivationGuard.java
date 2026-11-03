package com.englishplatform.catalog.internal;

import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDeactivationGuard;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Không cho ngừng kích hoạt giáo viên còn ghi danh {@code ACTIVE}/{@code PENDING_PAYMENT} (FR-004).
 * Học viên đã ẩn danh không còn được dạy nên không giữ giáo viên lại.
 */
@Component
class TeacherDeactivationGuard implements UserDeactivationGuard {

  private final EnrollmentRepository enrollments;
  private final UserDirectory userDirectory;

  TeacherDeactivationGuard(EnrollmentRepository enrollments, UserDirectory userDirectory) {
    this.enrollments = enrollments;
    this.userDirectory = userDirectory;
  }

  @Override
  public void check(UUID userId, Role role) {
    if (role == Role.TEACHER
        && enrollments
            .findStudentIdsByTeacher(
                userId, EnumSet.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PENDING_PAYMENT))
            .stream()
            .anyMatch(studentId -> !userDirectory.isAnonymized(studentId))) {
      throw new BusinessException(ErrorCode.TEACHER_HAS_STUDENTS);
    }
  }
}
