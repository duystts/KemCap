package com.englishplatform.scheduling.internal;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Kiểm tra vai trò/quyền sở hữu trong scheduling; không có quyền xem → 404 (không lộ dữ liệu). */
@Component
@EnableConfigurationProperties(SchedulingProperties.class)
public class SchedulingAccess {

  private final UserDirectory userDirectory;

  SchedulingAccess(UserDirectory userDirectory) {
    this.userDirectory = userDirectory;
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

  public static void requireAdminOrSelf(CurrentUser user, UUID userId) {
    if (!isAdmin(user) && !user.id().equals(userId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
  }

  public static BusinessException notFound() {
    return new BusinessException(ErrorCode.NOT_FOUND);
  }

  public UserView requireTeacher(UUID teacherId) {
    return userDirectory
        .find(teacherId)
        .filter(u -> u.role() == Role.TEACHER)
        .orElseThrow(SchedulingAccess::notFound);
  }

  /** Lịch rảnh/slot tính theo múi giờ của giáo viên. */
  public ZoneId teacherZone(UUID teacherId) {
    return ZoneId.of(requireTeacher(teacherId).timezone());
  }
}
