package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.identity.Role;
import com.englishplatform.scheduling.internal.session.CancellationService;
import com.englishplatform.scheduling.internal.session.OutcomeService;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Chốt kết quả, Gián đoạn, sửa kết quả, và đối chiếu bộ đếm với lịch sử buổi (NFR-REL-02). */
class SessionOutcomeIT extends SchedulingTestSupport {

  @Autowired OutcomeService outcomes;
  @Autowired CancellationService cancellation;

  @Test
  void onlySessionTeacherOrAdminRecordsOutcomeAfterStart() throws Exception {
    Setup s = setup(4);
    Instant startsAt = at(2, 10, 0);
    UUID id = bookOk(s.student(), s.enrollmentId(), startsAt);
    String done = "{\"outcome\":\"COMPLETED\"}";

    call(post("/api/v1/sessions/" + id + "/outcome"), s.teacher(), done)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    clock.set(startsAt.plus(Duration.ofMinutes(50)));
    call(post("/api/v1/sessions/" + id + "/outcome"), user(Role.TEACHER), done)
        .andExpect(status().isNotFound());
    call(post("/api/v1/sessions/" + id + "/outcome"), s.student(), done)
        .andExpect(status().isForbidden());
    call(post("/api/v1/sessions/" + id + "/outcome"), s.teacher(), done)
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.outcomeBy").value(s.teacher().id().toString()));
    assertThat(enrollment(s.enrollmentId()).sessionsConsumed()).isEqualTo(1);
    // giáo viên không sửa lại được; admin sửa trong 7 ngày
    call(
            post("/api/v1/sessions/" + id + "/outcome"),
            s.teacher(),
            "{\"outcome\":\"TEACHER_ABSENT\"}")
        .andExpect(status().isConflict());
    call(
            post("/api/v1/sessions/" + id + "/outcome"),
            s.admin(),
            "{\"outcome\":\"TEACHER_ABSENT\",\"note\":\"GV báo vắng\"}")
        .andExpect(jsonPath("$.status").value("TEACHER_ABSENT"));
    assertThat(enrollment(s.enrollmentId()).sessionsConsumed()).isZero();
    clock.set(startsAt.plus(Duration.ofDays(8)));
    call(post("/api/v1/sessions/" + id + "/outcome"), s.admin(), done)
        .andExpect(status().isConflict());
    // nhật ký: GV chốt COMPLETED, admin sửa sang TEACHER_ABSENT kèm ghi chú
    call(get("/api/v1/sessions/" + id + "/outcome-history"), s.admin())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].fromStatus").value("BOOKED"))
        .andExpect(jsonPath("$[0].toStatus").value("COMPLETED"))
        .andExpect(jsonPath("$[1].fromStatus").value("COMPLETED"))
        .andExpect(jsonPath("$[1].toStatus").value("TEACHER_ABSENT"))
        .andExpect(jsonPath("$[1].note").value("GV báo vắng"))
        .andExpect(jsonPath("$[1].changedBy").value(s.admin().id().toString()));
    call(get("/api/v1/sessions/" + id + "/outcome-history"), s.teacher())
        .andExpect(status().isForbidden());
  }

  /** R1 verify U3: admin không biến buổi GV đã hủy trước giờ thành "đã học". */
  @Test
  void teacherCancelledSessionIsNotCorrectable() throws Exception {
    Setup s = setup(4);
    Instant startsAt = at(2, 10, 0);
    UUID id = bookOk(s.student(), s.enrollmentId(), startsAt);
    call(post("/api/v1/sessions/" + id + "/cancel"), s.teacher(), "{}")
        .andExpect(jsonPath("$.status").value("TEACHER_ABSENT"));
    clock.set(startsAt.plus(Duration.ofHours(1)));
    call(post("/api/v1/sessions/" + id + "/outcome"), s.admin(), "{\"outcome\":\"COMPLETED\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    assertThat(enrollment(s.enrollmentId()).sessionsConsumed()).isZero();
  }

  @Test
  void adminSettlesOverdueAndDecidesInterruptions() throws Exception {
    Setup s = setup(4);
    Instant first = at(2, 8, 0);
    UUID overdue = bookOk(s.student(), s.enrollmentId(), first);
    UUID interrupted = bookOk(s.student(), s.enrollmentId(), at(2, 10, 0));
    UUID makeUp = bookOk(s.student(), s.enrollmentId(), at(2, 12, 0));
    clock.set(at(2, 14, 0));

    call(get("/api/v1/sessions?outcomePending=true"), s.teacher())
        .andExpect(jsonPath("$.length()").value(3));
    call(
            post("/api/v1/sessions/" + overdue + "/outcome"),
            s.admin(),
            "{\"outcome\":\"STUDENT_NO_SHOW\"}")
        .andExpect(jsonPath("$.outcomeBy").value(s.admin().id().toString()));
    for (UUID id : List.of(interrupted, makeUp)) {
      call(
              post("/api/v1/sessions/" + id + "/outcome"),
              s.teacher(),
              "{\"outcome\":\"INTERRUPTED\"}")
          .andExpect(jsonPath("$.status").value("INTERRUPTED"));
    }
    call(
            post("/api/v1/sessions/" + interrupted + "/interruption-decision"),
            s.admin(),
            "{\"decision\":\"COUNT\"}")
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    call(
            post("/api/v1/sessions/" + makeUp + "/interruption-decision"),
            s.admin(),
            "{\"decision\":\"MAKE_UP\"}")
        .andExpect(jsonPath("$.status").value("INTERRUPTED"));
    call(
            post("/api/v1/sessions/" + makeUp + "/interruption-decision"),
            s.teacher(),
            "{\"decision\":\"COUNT\"}")
        .andExpect(status().isForbidden());

    EnrollmentView e = enrollment(s.enrollmentId());
    assertThat(e.sessionsReserved()).isZero();
    assertThat(e.sessionsConsumed()).isEqualTo(2); // vắng + gián đoạn tính
    assertThat(e.sessionsRemaining()).isEqualTo(2); // xếp bù được trả chỗ
  }

  /** Chuỗi đặt/hủy/kết thúc ngẫu nhiên → bộ đếm ghi danh = đếm theo trạng thái buổi (SC-007). */
  @Test
  void countersAlwaysReconcileWithSessionHistory() throws Exception {
    Random random = new Random(42);
    Setup s = setup(30);
    CurrentUser admin = actor(s.admin());
    CurrentUser teacher = actor(s.teacher());
    CurrentUser student = actor(s.student());
    List<UUID> booked = new ArrayList<>();
    for (int day = 2; day < 20; day++) {
      booked.add(bookOk(s.student(), s.enrollmentId(), at(day, 6 + random.nextInt(14), 0)));
    }
    for (UUID id : booked) {
      Instant startsAt = sessions.findById(id).orElseThrow().getStartsAt();
      try {
        switch (random.nextInt(6)) {
          case 0 -> cancellation.cancel(student, id, null); // đúng hạn
          case 1 -> {
            clock.set(startsAt.minus(Duration.ofHours(2)));
            cancellation.cancel(student, id, null); // muộn
          }
          case 2 -> cancellation.cancel(teacher, id, null);
          case 3 -> {
            clock.set(startsAt.plus(Duration.ofMinutes(60)));
            SessionStatus outcome =
                List.copyOf(
                        EnumSet.of(
                            SessionStatus.COMPLETED,
                            SessionStatus.STUDENT_NO_SHOW,
                            SessionStatus.INTERRUPTED,
                            SessionStatus.TEACHER_ABSENT))
                    .get(random.nextInt(4));
            outcomes.record(teacher, id, outcome, null);
            if (outcome == SessionStatus.INTERRUPTED && random.nextBoolean()) {
              outcomes.decideInterruption(admin, id, true);
            }
          }
          case 4 -> {
            clock.set(startsAt.plus(Duration.ofHours(30)));
            outcomes.record(admin, id, SessionStatus.COMPLETED, "Chốt thay"); // chốt thay
          }
          default -> {} // để nguyên BOOKED
        }
      } catch (BusinessException ignored) {
        // thao tác không hợp lệ ở thời điểm đó vẫn không được làm lệch bộ đếm
      } finally {
        clock.reset();
      }
    }
    var all = sessions.findByEnrollmentId(s.enrollmentId());
    long reserved = all.stream().filter(x -> x.getStatus() == SessionStatus.BOOKED).count();
    long consumed =
        all.stream()
            .filter(
                x ->
                    EnumSet.of(
                            SessionStatus.COMPLETED,
                            SessionStatus.STUDENT_NO_SHOW,
                            SessionStatus.CANCELLED_LATE)
                        .contains(x.getStatus()))
            .count();
    EnrollmentView e = enrollment(s.enrollmentId());
    assertThat(e.sessionsReserved()).isEqualTo(reserved);
    assertThat(e.sessionsConsumed()).isEqualTo(consumed);
    assertThat(reserved + consumed).isLessThanOrEqualTo(30);
  }
}
