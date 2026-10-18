package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishplatform.scheduling.VideoRoomProvider.JoinInfo;
import com.englishplatform.scheduling.VideoRoomProvider.ParticipantRole;
import com.englishplatform.scheduling.internal.SchedulingProperties;
import com.englishplatform.scheduling.internal.session.JoinService;
import com.englishplatform.scheduling.internal.session.Session;
import com.englishplatform.scheduling.internal.session.SessionRepository;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import com.englishplatform.support.TestClockConfig.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Vào phòng (FR-012, NFR-SEC-08): đúng 2 người, đúng khung [−10 phút, kết thúc + 15 phút]. */
class JoinServiceTest {

  static final Instant START = Instant.parse("2026-11-05T12:00:00Z");

  SessionRepository sessions = mock(SessionRepository.class);
  VideoRoomProvider video = mock(VideoRoomProvider.class);
  MutableClock clock = new MutableClock(START);
  JoinService service =
      new JoinService(sessions, video, new SchedulingProperties(12, 2, 30, 10, 15), clock);

  UUID teacher = UUID.randomUUID();
  UUID student = UUID.randomUUID();
  Session session = new Session(UUID.randomUUID(), teacher, student, START, 45, student, null);

  @BeforeEach
  void setUp() {
    when(sessions.findById(session.getId())).thenReturn(Optional.of(session));
    when(video.issueJoin(any(), any(), any(), any(), any()))
        .thenReturn(new JoinInfo("ws://x", "t", "session-" + session.getId()));
  }

  @Test
  void participantsJoinInsideWindowWithTokenUntilRoomCloses() {
    clock.set(START.minus(Duration.ofMinutes(10)));
    service.join(user(teacher, "TEACHER"), session.getId());
    Instant closes = START.plus(Duration.ofMinutes(45 + 15));
    verify(video)
        .issueJoin(
            eq(session.getId()), eq(teacher), any(), eq(ParticipantRole.TEACHER), eq(closes));
    // ghi lần vào đầu bằng update có điều kiện (không tăng @Version — R3 verify U3)
    verify(sessions).markTeacherJoined(session.getId(), clock.instant());

    clock.set(closes.minusSeconds(1));
    service.join(user(student, "STUDENT"), session.getId());
    verify(sessions).markStudentJoined(session.getId(), clock.instant());
    assertThat(session.getVersion()).isZero();
  }

  @Test
  void outsideWindowIsRoomNotOpen() {
    clock.set(START.minus(Duration.ofMinutes(11)));
    assertCode(
        () -> service.join(user(student, "STUDENT"), session.getId()), ErrorCode.ROOM_NOT_OPEN);
    clock.set(START.plus(Duration.ofMinutes(60)));
    assertCode(
        () -> service.join(user(student, "STUDENT"), session.getId()), ErrorCode.ROOM_NOT_OPEN);
  }

  @Test
  void strangerGets404() {
    clock.set(START);
    assertCode(
        () -> service.join(user(UUID.randomUUID(), "TEACHER"), session.getId()),
        ErrorCode.NOT_FOUND);
  }

  @Test
  void cancelledSessionRoomIsClosed() {
    session.cancelByTeacher(teacher, "Ốm");
    clock.set(START);
    assertCode(
        () -> service.join(user(student, "STUDENT"), session.getId()), ErrorCode.ROOM_NOT_OPEN);
  }

  private static CurrentUser user(UUID id, String role) {
    return new CurrentUser(id, role, "Tên", "Asia/Ho_Chi_Minh");
  }

  private static void assertCode(Runnable action, ErrorCode code) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).code())
        .isEqualTo(code);
  }
}
