package com.englishplatform.catalog.internal;

import com.englishplatform.catalog.internal.enrollment.Enrollment;
import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import com.englishplatform.identity.Role;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Quyền đọc dữ liệu học viên theo vai trò (FR-003): admin đọc mọi người; giáo viên chỉ học viên
 * được phân công; học viên chỉ chính mình. Không có quyền → 404 (không lộ dữ liệu tồn tại).
 */
@Component
public class CatalogAccess {

  private final EnrollmentRepository enrollments;

  CatalogAccess(EnrollmentRepository enrollments) {
    this.enrollments = enrollments;
  }

  public static boolean isAdmin(CurrentUser user) {
    return user.hasRole(Role.ADMIN.name());
  }

  public static boolean isTeacher(CurrentUser user) {
    return user.hasRole(Role.TEACHER.name());
  }

  public static boolean isStudent(CurrentUser user) {
    return user.hasRole(Role.STUDENT.name());
  }

  public void requireCanReadStudent(CurrentUser user, UUID studentId) {
    boolean allowed =
        isAdmin(user)
            || (isStudent(user) && user.id().equals(studentId))
            || (isTeacher(user)
                && enrollments.existsByTeacherIdAndStudentIdAndStatusIn(
                    user.id(), studentId, Enrollment.ASSIGNED));
    if (!allowed) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
  }

  /** Admin; giáo viên đang phụ trách; học viên của ghi danh. */
  public void requireCanRead(CurrentUser user, Enrollment enrollment) {
    boolean allowed =
        isAdmin(user)
            || user.id().equals(enrollment.getStudentId())
            || user.id().equals(enrollment.getTeacherId());
    if (!allowed) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
  }
}
