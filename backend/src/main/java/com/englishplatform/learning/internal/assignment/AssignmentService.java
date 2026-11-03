package com.englishplatform.learning.internal.assignment;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.identity.Role;
import com.englishplatform.learning.LearningEvents.AssignmentGraded;
import com.englishplatform.learning.LearningEvents.AssignmentSubmitted;
import com.englishplatform.learning.internal.StudentAccessGuard;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bài tập (FR-020): học viên nộp văn bản, giáo viên đang phụ trách chấm 0–10 kèm phản hồi. */
@Service
public class AssignmentService {

  static final int MAX_ANSWER_LENGTH = 20_000;

  public enum Status {
    PENDING,
    SUBMITTED,
    GRADED
  }

  /** Bài tập kèm bài nộp (nếu có). */
  public record View(Assignment assignment, Submission submission) {

    public Status status() {
      if (submission == null) {
        return Status.PENDING;
      }
      return submission.isGraded() ? Status.GRADED : Status.SUBMITTED;
    }
  }

  private final AssignmentRepository assignments;
  private final SubmissionRepository submissions;
  private final EnrollmentApi enrollmentApi;
  private final StudentAccessGuard guard;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  AssignmentService(
      AssignmentRepository assignments,
      SubmissionRepository submissions,
      EnrollmentApi enrollmentApi,
      StudentAccessGuard guard,
      ApplicationEventPublisher events,
      Clock clock) {
    this.assignments = assignments;
    this.submissions = submissions;
    this.enrollmentApi = enrollmentApi;
    this.guard = guard;
    this.events = events;
    this.clock = clock;
  }

  /** Không lọc học viên: admin mọi bài, giáo viên học viên đang phụ trách, học viên của mình. */
  @Transactional(readOnly = true)
  public List<View> list(CurrentUser actor, UUID studentId, Status status) {
    List<Assignment> found;
    if (studentId != null) {
      guard.requireRead(actor, studentId);
      found = assignments.findByStudentIdInOrderByCreatedAtDesc(List.of(studentId));
    } else if (actor.hasRole(Role.ADMIN.name())) {
      found = assignments.findAllByOrderByCreatedAtDesc();
    } else if (actor.hasRole(Role.TEACHER.name())) {
      found =
          assignments.findByStudentIdInOrderByCreatedAtDesc(
              enrollmentApi.findAssignedStudentIds(actor.id()));
    } else {
      found = assignments.findByStudentIdInOrderByCreatedAtDesc(List.of(actor.id()));
    }
    Map<UUID, Submission> byAssignment =
        submissions.findByAssignmentIdIn(found.stream().map(Assignment::getId).toList()).stream()
            .collect(Collectors.toMap(Submission::getAssignmentId, Function.identity()));
    return found.stream()
        .map(a -> new View(a, byAssignment.get(a.getId())))
        .filter(v -> status == null || v.status() == status)
        .toList();
  }

  @Transactional
  public View submit(CurrentUser actor, UUID assignmentId, String textAnswer) {
    Assignment assignment = find(assignmentId);
    if (!assignment.getStudentId().equals(actor.id())) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    if (textAnswer == null || textAnswer.isBlank() || textAnswer.length() > MAX_ANSWER_LENGTH) {
      throw BusinessException.validation(
          "textAnswer", "Bài nộp cần nội dung, tối đa " + MAX_ANSWER_LENGTH + " ký tự");
    }
    Instant now = clock.instant();
    Submission submission =
        submissions
            .findByAssignmentId(assignmentId)
            .map(
                existing -> {
                  existing.resubmit(textAnswer.trim(), now);
                  return existing;
                })
            .orElseGet(() -> firstSubmission(assignmentId, textAnswer.trim(), now));
    teachersToNotify(assignment)
        .forEach(
            teacherId ->
                events.publishEvent(
                    new AssignmentSubmitted(assignmentId, assignment.getStudentId(), teacherId)));
    return new View(assignment, submission);
  }

  @Transactional
  public View grade(CurrentUser actor, UUID assignmentId, int score, String feedback) {
    Assignment assignment = find(assignmentId);
    guard.requireWrite(actor, assignment.getStudentId());
    if (score < 0 || score > 10) {
      throw BusinessException.validation("score", "Điểm từ 0 đến 10");
    }
    Submission submission =
        submissions
            .findByAssignmentId(assignmentId)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.INVALID_STATE_TRANSITION, "Học viên chưa nộp bài"));
    submission.grade(score, feedback, actor.id(), clock.instant());
    events.publishEvent(new AssignmentGraded(assignmentId, assignment.getStudentId(), score));
    return new View(assignment, submission);
  }

  private Submission firstSubmission(UUID assignmentId, String textAnswer, Instant now) {
    try {
      return submissions.saveAndFlush(new Submission(assignmentId, textAnswer, now));
    } catch (DataIntegrityViolationException e) {
      // hai lượt nộp đầu chen nhau: unique assignment_id chặn lượt sau
      throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
    }
  }

  /** Giáo viên giao bài nếu còn phụ trách; đã đổi giáo viên thì báo người chấm được bài này. */
  private List<UUID> teachersToNotify(Assignment assignment) {
    List<UUID> assigned = enrollmentApi.findAssignedTeacherIds(assignment.getStudentId());
    return assigned.contains(assignment.getTeacherId())
        ? List.of(assignment.getTeacherId())
        : assigned;
  }

  private Assignment find(UUID id) {
    return assignments.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
