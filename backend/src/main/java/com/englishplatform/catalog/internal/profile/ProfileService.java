package com.englishplatform.catalog.internal.profile;

import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.GoalStatusProvider;
import com.englishplatform.catalog.internal.CatalogAccess;
import com.englishplatform.catalog.internal.enrollment.Enrollment;
import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserStatus;
import com.englishplatform.identity.UserView;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hồ sơ giáo viên/học viên và danh sách học viên (FR-003, FR-004, FR-007). Hồ sơ chưa từng lưu được
 * trả về rỗng; lần {@code PUT} đầu tiên tạo bản ghi.
 */
@Service
public class ProfileService {

  private final TeacherProfileRepository teacherProfiles;
  private final StudentProfileRepository studentProfiles;
  private final EnrollmentRepository enrollments;
  private final UserDirectory userDirectory;
  private final CatalogAccess access;
  private final ObjectProvider<GoalStatusProvider> goalStatus;

  ProfileService(
      TeacherProfileRepository teacherProfiles,
      StudentProfileRepository studentProfiles,
      EnrollmentRepository enrollments,
      UserDirectory userDirectory,
      CatalogAccess access,
      ObjectProvider<GoalStatusProvider> goalStatus) {
    this.teacherProfiles = teacherProfiles;
    this.studentProfiles = studentProfiles;
    this.enrollments = enrollments;
    this.userDirectory = userDirectory;
    this.access = access;
    this.goalStatus = goalStatus;
  }

  /** Ghi danh làm học viên "đang học" khi tính cờ "Cần tư vấn". */
  static final Set<EnrollmentStatus> STUDYING =
      EnumSet.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.PENDING_PAYMENT);

  /**
   * Một dòng trong danh sách học viên. {@code needsConsulting}: đang có ghi danh {@code
   * ACTIVE}/{@code PENDING_PAYMENT} mà chưa có mục tiêu học tập {@code ACTIVE}.
   */
  public record StudentSummary(
      UserView user,
      StudentProfile.CefrLevel selfReportedLevel,
      Set<UUID> teacherIds,
      boolean needsConsulting) {}

  @Transactional(readOnly = true)
  public TeacherProfile teacherProfile(CurrentUser actor, UUID teacherId) {
    requireAdminOrSelf(actor, teacherId);
    requireUser(teacherId, Role.TEACHER);
    return teacherProfiles.findById(teacherId).orElseGet(() -> new TeacherProfile(teacherId));
  }

  @Transactional
  public TeacherProfile updateTeacherProfile(
      CurrentUser actor,
      UUID teacherId,
      String bio,
      List<String> specialties,
      String certificates) {
    requireAdminOrSelf(actor, teacherId);
    requireUser(teacherId, Role.TEACHER);
    TeacherProfile profile =
        teacherProfiles.findById(teacherId).orElseGet(() -> new TeacherProfile(teacherId));
    profile.update(bio, specialties, certificates);
    return teacherProfiles.save(profile);
  }

  @Transactional(readOnly = true)
  public StudentProfile studentProfile(CurrentUser actor, UUID studentId) {
    access.requireCanReadStudent(actor, studentId);
    requireUser(studentId, Role.STUDENT);
    return studentProfiles.findById(studentId).orElseGet(() -> new StudentProfile(studentId));
  }

  /** Chỉ admin (controller đã chặn vai trò). */
  @Transactional
  public StudentProfile updateStudentProfile(
      UUID studentId, StudentProfile.CefrLevel level, String occupation, String notes) {
    requireUser(studentId, Role.STUDENT);
    StudentProfile profile =
        studentProfiles.findById(studentId).orElseGet(() -> new StudentProfile(studentId));
    profile.update(level, occupation, notes);
    return studentProfiles.save(profile);
  }

  /**
   * Admin: mọi học viên (lọc {@code teacherId} nếu có). Giáo viên: chỉ học viên được phân công.
   * {@code needsConsulting} (tùy chọn) lọc theo cờ "Cần tư vấn".
   */
  @Transactional(readOnly = true)
  public List<StudentSummary> listStudents(
      CurrentUser actor, String q, UUID teacherId, UserStatus status, Boolean needsConsulting) {
    UUID scopeTeacher = CatalogAccess.isTeacher(actor) ? actor.id() : teacherId;
    if (!CatalogAccess.isAdmin(actor) && !CatalogAccess.isTeacher(actor)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    Set<UUID> allowed =
        scopeTeacher == null
            ? null
            : new HashSet<>(enrollments.findStudentIdsByTeacher(scopeTeacher, Enrollment.ASSIGNED));
    List<UserView> students =
        userDirectory.search(Role.STUDENT, status, q).stream()
            .filter(u -> allowed == null || allowed.contains(u.id()))
            .toList();
    List<UUID> ids = students.stream().map(UserView::id).toList();
    Map<UUID, StudentProfile> profiles =
        studentProfiles.findAllById(ids).stream()
            .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
    List<Enrollment> studentEnrollments = enrollments.findByStudentIdIn(ids);
    Map<UUID, Set<UUID>> teachers =
        studentEnrollments.stream()
            .filter(e -> Enrollment.ASSIGNED.contains(e.getStatus()))
            .collect(
                Collectors.groupingBy(
                    Enrollment::getStudentId,
                    Collectors.mapping(Enrollment::getTeacherId, Collectors.toSet())));
    Set<UUID> studying =
        studentEnrollments.stream()
            .filter(e -> STUDYING.contains(e.getStatus()))
            .map(Enrollment::getStudentId)
            .collect(Collectors.toSet());
    GoalStatusProvider provider = goalStatus.getIfAvailable();
    Set<UUID> withGoal = provider == null ? Set.of() : provider.studentsWithActiveGoal(studying);
    return students.stream()
        .map(
            u ->
                new StudentSummary(
                    u,
                    profiles.containsKey(u.id())
                        ? profiles.get(u.id()).getSelfReportedLevel()
                        : null,
                    teachers.getOrDefault(u.id(), Set.of()),
                    provider != null && studying.contains(u.id()) && !withGoal.contains(u.id())))
        .filter(s -> needsConsulting == null || s.needsConsulting() == needsConsulting)
        .toList();
  }

  private static void requireAdminOrSelf(CurrentUser actor, UUID userId) {
    if (!CatalogAccess.isAdmin(actor) && !actor.id().equals(userId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
  }

  private void requireUser(UUID userId, Role role) {
    userDirectory
        .find(userId)
        .filter(u -> u.role() == role)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
