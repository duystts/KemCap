package com.englishplatform.scheduling.internal.session;

import com.englishplatform.scheduling.SessionStatus;
import com.englishplatform.scheduling.VideoRoomProvider;
import com.englishplatform.scheduling.VideoRoomProvider.JoinInfo;
import com.englishplatform.scheduling.VideoRoomProvider.ParticipantRole;
import com.englishplatform.scheduling.internal.SchedulingAccess;
import com.englishplatform.scheduling.internal.SchedulingProperties;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vào phòng học (FR-012, NFR-SEC-08): chỉ giáo viên và học viên của buổi (người khác → 404), chỉ
 * trong [bắt đầu − 10 phút, kết thúc + 15 phút] (ngoài khung → 403 {@code ROOM_NOT_OPEN}); token
 * hết hạn lúc đóng cửa sổ.
 */
@Service
public class JoinService {

  private final SessionRepository sessions;
  private final VideoRoomProvider videoRoomProvider;
  private final SchedulingProperties props;
  private final Clock clock;

  public JoinService(
      SessionRepository sessions,
      VideoRoomProvider videoRoomProvider,
      SchedulingProperties props,
      Clock clock) {
    this.sessions = sessions;
    this.videoRoomProvider = videoRoomProvider;
    this.props = props;
    this.clock = clock;
  }

  @Transactional
  public JoinInfo join(CurrentUser actor, UUID sessionId) {
    Session session = sessions.findById(sessionId).orElseThrow(SchedulingAccess::notFound);
    if (!session.isParticipant(actor.id())) {
      throw SchedulingAccess.notFound();
    }
    Instant now = clock.instant();
    Instant opensAt = session.getStartsAt().minus(props.roomOpenBefore());
    Instant closesAt = session.getEndsAt().plus(props.roomCloseAfter());
    if (session.getStatus() != SessionStatus.BOOKED
        || now.isBefore(opensAt)
        || !now.isBefore(closesAt)) {
      throw new BusinessException(ErrorCode.ROOM_NOT_OPEN);
    }
    boolean teacher = actor.id().equals(session.getTeacherId());
    if (teacher) {
      sessions.markTeacherJoined(session.getId(), now);
    } else {
      sessions.markStudentJoined(session.getId(), now);
    }
    return videoRoomProvider.issueJoin(
        session.getId(),
        actor.id(),
        actor.name(),
        teacher ? ParticipantRole.TEACHER : ParticipantRole.STUDENT,
        closesAt);
  }
}
