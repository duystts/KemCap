package com.englishplatform.catalog.internal.enrollment;

import com.englishplatform.catalog.CatalogEvents.EnrollmentCreated;
import com.englishplatform.catalog.CatalogEvents.TeacherReassigned;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.internal.CatalogAccess;
import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ghi danh (FR-006, FR-024): tạo, đổi giáo viên, gia hạn, hủy, xem theo quyền. */
@Service
public class EnrollmentService {

  private final EnrollmentRepository enrollments;
  private final TeacherAssignmentHistoryRepository history;
  private final CourseService courseService;
  private final UserDirectory userDirectory;
  private final CatalogAccess access;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  EnrollmentService(
      EnrollmentRepository enrollments,
      TeacherAssignmentHistoryRepository history,
      CourseService courseService,
      UserDirectory userDirectory,
      CatalogAccess access,
      ApplicationEventPublisher events,
      Clock clock) {
    this.enrollments = enrollments;
    this.history = history;
    this.courseService = courseService;
    this.userDirectory = userDirectory;
    this.access = access;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Tạo ghi danh {@code PENDING_PAYMENT}: sao chép số buổi, thời lượng, hạn dùng, giá từ gói;
   * {@code priceVnd} (giảm giá) chỉ nhập được lúc này. Học viên đã mời nhưng chưa kích hoạt vẫn ghi
   * danh được; giáo viên phải {@code ACTIVE}.
   */
  @Transactional
  public Enrollment create(UUID studentId, UUID packageId, UUID teacherId, Long priceVnd, UUID by) {
    userDirectory
        .find(studentId)
        .filter(u -> u.role() == Role.STUDENT && u.status() != UserStatus.INACTIVE)
        .orElseThrow(
            () ->
                BusinessException.validation("studentId", "Học viên không tồn tại hoặc đã ngừng"));
    userDirectory.requireActive(teacherId, Role.TEACHER);
    CoursePackage pkg = courseService.requireActivePackage(packageId);
    long price = priceVnd == null ? pkg.getPriceVnd() : priceVnd;
    if (price < 0) {
      throw BusinessException.validation("priceVnd", "Giá không được âm");
    }
    Enrollment enrollment =
        enrollments.save(
            new Enrollment(
                studentId,
                packageId,
                teacherId,
                pkg.getSessionCount(),
                pkg.getSessionMinutes(),
                pkg.getValidityDays(),
                price));
    history.save(new TeacherAssignmentHistory(enrollment.getId(), null, teacherId, by, null));
    events.publishEvent(new EnrollmentCreated(enrollment.getId(), studentId, teacherId, price));
    return enrollment;
  }

  /** Admin: theo bộ lọc. Giáo viên: chỉ ghi danh mình phụ trách. Học viên: chỉ của mình. */
  @Transactional(readOnly = true)
  public List<Enrollment> list(
      CurrentUser actor, UUID studentId, UUID teacherId, EnrollmentStatus status) {
    if (CatalogAccess.isTeacher(actor)) {
      teacherId = actor.id();
    } else if (CatalogAccess.isStudent(actor)) {
      studentId = actor.id();
    }
    return enrollments.search(studentId, teacherId, status);
  }

  @Transactional(readOnly = true)
  public Enrollment get(CurrentUser actor, UUID id) {
    Enrollment enrollment = find(id);
    access.requireCanRead(actor, enrollment);
    return enrollment;
  }

  /**
   * Đổi giáo viên phụ trách (FR-006): ghi lịch sử, phát {@link TeacherReassigned} — scheduling hủy
   * hệ thống các buổi tương lai với giáo viên cũ (U3).
   */
  @Transactional
  public Enrollment changeTeacher(UUID id, UUID newTeacherId, String reason, UUID by) {
    if (reason == null || reason.isBlank()) {
      throw BusinessException.validation("reason", "Cần ghi lý do đổi giáo viên");
    }
    userDirectory.requireActive(newTeacherId, Role.TEACHER);
    Enrollment enrollment = find(id);
    UUID from = enrollment.getTeacherId();
    enrollment.changeTeacher(newTeacherId);
    history.save(new TeacherAssignmentHistory(id, from, newTeacherId, by, reason.trim()));
    events.publishEvent(new TeacherReassigned(id, enrollment.getStudentId(), from, newTeacherId));
    return enrollment;
  }

  /** Gia hạn chỉ khi giáo viên phụ trách còn ACTIVE — nếu không, đổi giáo viên trước. */
  @Transactional
  public Enrollment extend(UUID id, Instant newExpiresAt, UUID by) {
    Enrollment enrollment = find(id);
    if (userDirectory
        .find(enrollment.getTeacherId())
        .filter(u -> u.status() == UserStatus.ACTIVE)
        .isEmpty()) {
      throw BusinessException.validation(
          "teacherId", "Giáo viên phụ trách đã ngừng, hãy đổi giáo viên trước khi gia hạn");
    }
    enrollment.extend(newExpiresAt, by, clock.instant());
    return enrollment;
  }

  @Transactional
  public Enrollment cancel(UUID id) {
    Enrollment enrollment = find(id);
    enrollment.cancel();
    return enrollment;
  }

  @Transactional(readOnly = true)
  public List<TeacherAssignmentHistory> history(UUID id) {
    return history.findByEnrollmentIdOrderByCreatedAt(id);
  }

  private Enrollment find(UUID id) {
    return enrollments.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
