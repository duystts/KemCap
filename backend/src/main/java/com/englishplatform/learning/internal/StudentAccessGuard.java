package com.englishplatform.learning.internal;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Quyền trên dữ liệu học tập của một học viên (FR-003): admin mọi học viên; giáo viên khi được phân
 * công ({@link EnrollmentApi#isAssigned}); học viên chỉ đọc của mình. Không có quyền xem → 404; học
 * viên ghi → 403.
 */
@Component
public class StudentAccessGuard {

  private final EnrollmentApi enrollmentApi;
  private final UserDirectory userDirectory;

  StudentAccessGuard(EnrollmentApi enrollmentApi, UserDirectory userDirectory) {
    this.enrollmentApi = enrollmentApi;
    this.userDirectory = userDirectory;
  }

  public void requireRead(CurrentUser actor, UUID studentId) {
    requireStudent(studentId);
    boolean allowed =
        actor.hasRole(Role.ADMIN.name())
            || (actor.hasRole(Role.STUDENT.name()) && actor.id().equals(studentId))
            || (actor.hasRole(Role.TEACHER.name())
                && enrollmentApi.isAssigned(actor.id(), studentId));
    if (!allowed) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
  }

  public void requireWrite(CurrentUser actor, UUID studentId) {
    requireRead(actor, studentId);
    if (actor.hasRole(Role.STUDENT.name())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
  }

  private void requireStudent(UUID studentId) {
    userDirectory
        .find(studentId)
        .filter(u -> u.role() == Role.STUDENT)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
