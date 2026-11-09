package com.englishplatform.learning.internal.report;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.learning.LearningEvents.SessionReportSubmitted;
import com.englishplatform.learning.internal.StudentAccessGuard;
import com.englishplatform.learning.internal.assignment.Assignment;
import com.englishplatform.learning.internal.assignment.AssignmentRepository;
import com.englishplatform.learning.internal.path.LearningPath;
import com.englishplatform.learning.internal.path.LearningPathRepository;
import com.englishplatform.learning.internal.path.Topic;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu đánh giá buổi học (FR-019). Chỉ giáo viên của buổi nộp, kể cả khi học viên đã chuyển sang
 * giáo viên khác; nộp một lần trong một transaction cùng bài tập và chủ đề hoàn thành.
 */
@Service
public class SessionReportService {

  static final int MAX_TEXT_LENGTH = 5000;

  private final SessionReportRepository reports;
  private final AssignmentRepository assignments;
  private final LearningPathRepository paths;
  private final EnrollmentApi enrollmentApi;
  private final StudentAccessGuard guard;
  private final UserDirectory userDirectory;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  SessionReportService(
      SessionReportRepository reports,
      AssignmentRepository assignments,
      LearningPathRepository paths,
      EnrollmentApi enrollmentApi,
      StudentAccessGuard guard,
      UserDirectory userDirectory,
      ApplicationEventPublisher events,
      Clock clock) {
    this.reports = reports;
    this.assignments = assignments;
    this.paths = paths;
    this.enrollmentApi = enrollmentApi;
    this.guard = guard;
    this.userDirectory = userDirectory;
    this.events = events;
    this.clock = clock;
  }

  public record NewAssignment(String title, String instructions, Instant dueAt) {}

  public record Submission(
      List<UUID> taughtTopicIds,
      List<UUID> completedTopicIds,
      SessionReport.Scores scores,
      String comment,
      List<NewAssignment> assignments) {}

  /**
   * Admin: mọi phiếu. Giáo viên: phiếu của mình, nhưng phiếu đã nộp chỉ khi còn phụ trách học viên
   * (FR-003); phiếu chưa nộp vẫn hiện vì đó là việc của giáo viên đã dạy buổi đó.
   */
  @Transactional(readOnly = true)
  public List<SessionReport> list(CurrentUser actor, SessionReport.Status status) {
    if (actor.hasRole(Role.ADMIN.name())) {
      return reports.search(null, status);
    }
    Set<UUID> assigned = new HashSet<>(enrollmentApi.findAssignedStudentIds(actor.id()));
    return reports.search(actor.id(), status).stream()
        .filter(
            r ->
                r.getStatus() == SessionReport.Status.PENDING
                    || assigned.contains(r.getStudentId()))
        .toList();
  }

  @Transactional(readOnly = true)
  public SessionReport get(CurrentUser actor, UUID sessionId) {
    SessionReport report = find(sessionId);
    boolean ownPending =
        report.getStatus() == SessionReport.Status.PENDING
            && report.getTeacherId().equals(actor.id());
    if (!ownPending) {
      guard.requireRead(actor, report.getStudentId());
    }
    return report;
  }

  @Transactional
  public SessionReport submit(CurrentUser actor, UUID sessionId, Submission input) {
    SessionReport report = find(sessionId);
    if (!report.getTeacherId().equals(actor.id())) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    userDirectory.requireNotAnonymized(report.getStudentId());
    if (report.getStatus() != SessionReport.Status.PENDING) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Phiếu đã nộp, không nộp lại được");
    }
    validate(input);
    markTopics(actor.id(), report.getStudentId(), input);
    // chủ đề hoàn thành cũng là chủ đề đã dạy
    Set<UUID> taught = new LinkedHashSet<>(input.taughtTopicIds());
    taught.addAll(input.completedTopicIds());
    report.submit(input.scores(), List.copyOf(taught), input.comment().trim(), clock.instant());
    input
        .assignments()
        .forEach(
            a ->
                assignments.save(
                    new Assignment(
                        report.getId(),
                        report.getSessionId(),
                        report.getStudentId(),
                        report.getTeacherId(),
                        a.title().trim(),
                        a.instructions(),
                        a.dueAt())));
    events.publishEvent(new SessionReportSubmitted(sessionId, report.getStudentId()));
    return report;
  }

  /** Giáo viên của buổi còn phụ trách học viên: còn chọn được chủ đề, giáo vụ chưa đóng được. */
  @Transactional(readOnly = true)
  public boolean isTeacherAssigned(SessionReport report) {
    return enrollmentApi.isAssigned(report.getTeacherId(), report.getStudentId());
  }

  /**
   * Giáo vụ đóng phiếu chưa nộp khi giáo viên của buổi không còn phụ trách học viên, ví dụ đã nghỉ
   * việc (T148). Giáo viên còn phụ trách thì phải nộp, không đóng thay được.
   */
  @Transactional
  public SessionReport close(CurrentUser actor, UUID sessionId, String reason) {
    SessionReport report = find(sessionId);
    if (reason == null || reason.isBlank()) {
      throw BusinessException.validation("reason", "Cần lý do đóng phiếu");
    }
    if (reason.length() > MAX_TEXT_LENGTH) {
      throw BusinessException.validation("reason", "Lý do tối đa " + MAX_TEXT_LENGTH + " ký tự");
    }
    if (report.getStatus() != SessionReport.Status.PENDING) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Chỉ đóng được phiếu chưa nộp");
    }
    if (isTeacherAssigned(report)) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION,
          "Giáo viên của buổi vẫn phụ trách học viên, hãy nhắc giáo viên nộp phiếu");
    }
    report.close(reason.trim(), actor.id(), clock.instant());
    return report;
  }

  private static void validate(Submission input) {
    if (input.comment() == null || input.comment().isBlank()) {
      throw BusinessException.validation("comment", "Cần nhận xét buổi học");
    }
    if (input.comment().length() > MAX_TEXT_LENGTH) {
      throw BusinessException.validation(
          "comment", "Nhận xét tối đa " + MAX_TEXT_LENGTH + " ký tự");
    }
    if (input.scores() == null || input.scores().isEmpty()) {
      throw BusinessException.validation("scores", "Cần chấm ít nhất một kỹ năng");
    }
    if (!input.scores().inRange()) {
      throw BusinessException.validation("scores", "Điểm kỹ năng từ 1 đến 5");
    }
    for (NewAssignment a : input.assignments()) {
      if (a.title() == null || a.title().isBlank() || a.title().length() > 200) {
        throw BusinessException.validation("assignments", "Bài tập cần tiêu đề, tối đa 200 ký tự");
      }
      if (a.instructions() != null && a.instructions().length() > MAX_TEXT_LENGTH) {
        throw BusinessException.validation(
            "assignments", "Hướng dẫn bài tập tối đa " + MAX_TEXT_LENGTH + " ký tự");
      }
    }
  }

  /** Chủ đề phải thuộc lộ trình đang dùng của học viên; chủ đề hoàn thành chuyển {@code DONE}. */
  private void markTopics(UUID teacherId, UUID studentId, Submission input) {
    Set<UUID> requested = new HashSet<>(input.taughtTopicIds());
    requested.addAll(input.completedTopicIds());
    if (requested.isEmpty()) {
      return;
    }
    if (requested.contains(null)) {
      throw BusinessException.validation("taughtTopicIds", "Chủ đề không hợp lệ");
    }
    // giáo viên đã bị đổi chỉ nộp nhận xét của buổi mình dạy, không đụng tới lộ trình (plan.md,
    // Complexity Tracking)
    if (!enrollmentApi.isAssigned(teacherId, studentId)) {
      throw BusinessException.validation(
          "taughtTopicIds", "Bạn không còn phụ trách học viên, phiếu không được chọn chủ đề");
    }
    Map<UUID, Topic> topics =
        paths
            .findByStudentIdAndStatus(studentId, LearningPath.Status.ACTIVE)
            .map(
                path ->
                    path.getMilestones().stream()
                        .flatMap(m -> m.getTopics().stream())
                        .collect(Collectors.toMap(Topic::getId, Function.identity())))
            .orElse(Map.of());
    if (!topics.keySet().containsAll(requested)) {
      throw BusinessException.validation(
          "taughtTopicIds", "Chủ đề phải thuộc lộ trình của học viên");
    }
    input.completedTopicIds().forEach(id -> topics.get(id).markDone());
  }

  private SessionReport find(UUID sessionId) {
    return reports
        .findBySessionId(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
