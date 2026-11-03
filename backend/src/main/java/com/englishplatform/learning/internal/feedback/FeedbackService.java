package com.englishplatform.learning.internal.feedback;

import com.englishplatform.scheduling.SessionQueryApi;
import com.englishplatform.scheduling.SessionQueryApi.SessionView;
import com.englishplatform.scheduling.SessionStatus;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đánh giá sao (FR-022): buổi hoàn thành của chính học viên, trong 7 ngày, một lần. */
@Service
public class FeedbackService {

  static final Duration WINDOW = Duration.ofDays(7);

  private final SessionFeedbackRepository feedback;
  private final SessionQueryApi sessions;
  private final Clock clock;

  FeedbackService(SessionFeedbackRepository feedback, SessionQueryApi sessions, Clock clock) {
    this.feedback = feedback;
    this.sessions = sessions;
    this.clock = clock;
  }

  @Transactional
  public SessionFeedback rate(CurrentUser actor, UUID sessionId, int stars, String comment) {
    if (stars < 1 || stars > 5) {
      throw BusinessException.validation("stars", "Đánh giá từ 1 đến 5 sao");
    }
    SessionView session =
        sessions
            .find(sessionId)
            .filter(s -> s.studentId().equals(actor.id()))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (session.status() != SessionStatus.COMPLETED) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Chỉ đánh giá được buổi đã hoàn thành");
    }
    if (clock.instant().isAfter(session.endsAt().plus(WINDOW))) {
      throw new BusinessException(ErrorCode.FEEDBACK_WINDOW_CLOSED);
    }
    if (feedback.existsBySessionId(sessionId)) {
      throw new BusinessException(ErrorCode.FEEDBACK_EXISTS);
    }
    try {
      return feedback.saveAndFlush(
          new SessionFeedback(sessionId, actor.id(), session.teacherId(), stars, comment));
    } catch (DataIntegrityViolationException e) {
      // hai lượt gửi chen nhau: unique session_id chặn lượt sau
      throw new BusinessException(ErrorCode.FEEDBACK_EXISTS);
    }
  }

  @Transactional(readOnly = true)
  public List<SessionFeedback> mine(CurrentUser actor) {
    return feedback.findByStudentId(actor.id());
  }

  @Transactional(readOnly = true)
  public SessionFeedback get(UUID sessionId) {
    return feedback
        .findBySessionId(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }
}
