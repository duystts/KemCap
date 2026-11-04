package com.englishplatform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.Role;
import com.englishplatform.learning.LearningEvents.AssignmentGraded;
import com.englishplatform.learning.LearningEvents.AssignmentSubmitted;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** Bài tập (FR-020, K2 chỉ văn bản): nộp lại tới khi chấm, nộp muộn, chấm 0–10, phân quyền. */
@RecordApplicationEvents
class AssignmentFlowIT extends LearningFlowSupport {

  @Autowired ApplicationEvents events;

  private UUID assignment(Setup s, String dueAtField) throws Exception {
    UUID sessionId = completedWithPendingReport(s, 2);
    submitReport(
            s.teacher(),
            sessionId,
            SCORES
                + ",\"comment\":\"Tốt\",\"assignments\":[{\"title\":\"Viết đoạn văn\""
                + dueAtField
                + "}]")
        .andExpect(status().isOk());
    return UUID.fromString(
        read(
            call(get("/api/v1/assignments?studentId=" + s.student().id()), s.student()),
            "$[0].id"));
  }

  private org.springframework.test.web.servlet.ResultActions submit(
      TestUser as, UUID id, String text) throws Exception {
    return call(
        put("/api/v1/assignments/" + id + "/submission"), as, "{\"textAnswer\":\"" + text + "\"}");
  }

  private org.springframework.test.web.servlet.ResultActions grade(TestUser as, UUID id, int score)
      throws Exception {
    return call(
        post("/api/v1/assignments/" + id + "/grade"),
        as,
        "{\"score\":" + score + ",\"feedback\":\"Cần luyện thêm\"}");
  }

  @Test
  void studentSubmitsResubmitsUntilGradedThenSeesScore() throws Exception {
    Setup s = setup();
    UUID id = assignment(s, "");

    call(get("/api/v1/assignments?status=PENDING&studentId=" + s.student().id()), s.student())
        .andExpect(jsonPath("$.length()").value(1));
    submit(s.student(), id, " ").andExpect(status().isBadRequest());
    submit(s.student(), id, "Bản đầu")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"))
        .andExpect(jsonPath("$.submission.late").value(false));
    submit(s.student(), id, "Bản sửa")
        .andExpect(jsonPath("$.submission.textAnswer").value("Bản sửa"));

    grade(s.teacher(), id, 11).andExpect(status().isBadRequest());
    grade(s.teacher(), id, -1).andExpect(status().isBadRequest());
    grade(s.teacher(), id, 8)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("GRADED"))
        .andExpect(jsonPath("$.submission.score").value(8));

    assertThat(events.stream(AssignmentSubmitted.class).filter(e -> e.assignmentId().equals(id)))
        .isNotEmpty();
    assertThat(events.stream(AssignmentGraded.class).filter(e -> e.assignmentId().equals(id)))
        .hasSize(1);

    // đã chấm: không nộp lại, không chấm lại (MVP)
    submit(s.student(), id, "Bản ba")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    grade(s.teacher(), id, 9).andExpect(status().isConflict());
    call(get("/api/v1/assignments?studentId=" + s.student().id()), s.student())
        .andExpect(jsonPath("$[0].submission.feedback").value("Cần luyện thêm"))
        .andExpect(jsonPath("$[0].submission.textAnswer").value("Bản sửa"));
  }

  /** F1: bài nộp sau khi đổi giáo viên báo giáo viên đang phụ trách, người chấm được. */
  @Test
  void submissionNotifiesCurrentTeacherAfterReassignment() throws Exception {
    Setup s = setup();
    UUID id = assignment(s, "");
    TestUser newTeacher = user(Role.TEACHER);
    call(
            put("/api/v1/enrollments/" + s.enrollmentId() + "/teacher"),
            s.admin(),
            "{\"teacherId\":\"" + newTeacher.id() + "\",\"reason\":\"GV nghỉ\"}")
        .andExpect(status().isOk());

    submit(s.student(), id, "Bài của tôi").andExpect(status().isOk());

    assertThat(events.stream(AssignmentSubmitted.class).filter(e -> e.assignmentId().equals(id)))
        .extracting(AssignmentSubmitted::teacherId)
        .containsExactly(newTeacher.id());
    grade(newTeacher, id, 7).andExpect(status().isOk());
  }

  /** R3: bài tập ghép được với buổi học để FE hiện theo từng buổi. */
  @Test
  void assignmentLinksToSession() throws Exception {
    Setup s = setup();
    assignment(s, "");
    String sessionId =
        read(call(get("/api/v1/session-reports?status=SUBMITTED"), s.teacher()), "$[0].sessionId");
    call(get("/api/v1/assignments?studentId=" + s.student().id()), s.student())
        .andExpect(jsonPath("$[0].sessionId").value(sessionId));
  }

  /** F3: nộp lần đầu song song không trả 500; đúng một bài nộp. */
  @Test
  void concurrentFirstSubmissionsNeverFailWith500() throws Exception {
    Setup s = setup();
    UUID id = assignment(s, "");
    List<Integer> codes = new CopyOnWriteArrayList<>();
    CountDownLatch start = new CountDownLatch(1);
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      Thread t =
          new Thread(
              () -> {
                try {
                  start.await();
                  codes.add(submit(s.student(), id, "Bài").andReturn().getResponse().getStatus());
                } catch (Exception e) {
                  throw new IllegalStateException(e);
                }
              });
      t.start();
      threads.add(t);
    }
    start.countDown();
    for (Thread t : threads) {
      t.join();
    }

    assertThat(codes).hasSize(6).allMatch(c -> c == 200 || c == 409).contains(200);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from submission where assignment_id = ?", Long.class, id))
        .isEqualTo(1);
  }

  /** F11: phản hồi khi chấm có giới hạn độ dài. */
  @Test
  void oversizedFeedbackIsRejected() throws Exception {
    Setup s = setup();
    UUID id = assignment(s, "");
    submit(s.student(), id, "Bài").andExpect(status().isOk());
    call(
            post("/api/v1/assignments/" + id + "/grade"),
            s.teacher(),
            "{\"score\":5,\"feedback\":\"" + "x".repeat(5001) + "\"}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void cannotGradeBeforeSubmission() throws Exception {
    Setup s = setup();
    UUID id = assignment(s, "");
    grade(s.teacher(), id, 8)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
  }

  @Test
  void lateSubmissionIsAcceptedAndMarked() throws Exception {
    Setup s = setup();
    UUID id = assignment(s, ",\"dueAt\":\"" + clock.instant().plus(Duration.ofDays(1)) + "\"");
    clock.advance(Duration.ofDays(2));
    submit(s.student(), id, "Nộp muộn")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submission.late").value(true));
  }

  @Test
  void noDueDateIsNeverLate() throws Exception {
    Setup s = setup();
    UUID id = assignment(s, "");
    clock.advance(Duration.ofDays(30));
    submit(s.student(), id, "Rất lâu sau").andExpect(jsonPath("$.submission.late").value(false));
  }

  @Test
  void accessFollowsAssignment() throws Exception {
    Setup s = setup();
    UUID id = assignment(s, "");
    TestUser otherStudent = user(Role.STUDENT);
    TestUser otherTeacher = user(Role.TEACHER);

    submit(otherStudent, id, "Không phải của tôi").andExpect(status().isNotFound());
    submit(s.teacher(), id, "GV nộp hộ").andExpect(status().isForbidden());
    call(get("/api/v1/assignments?studentId=" + s.student().id()), otherStudent)
        .andExpect(status().isNotFound());
    call(get("/api/v1/assignments?studentId=" + s.student().id()), otherTeacher)
        .andExpect(status().isNotFound());
    // không lọc: chỉ thấy của mình
    call(get("/api/v1/assignments"), otherStudent).andExpect(jsonPath("$.length()").value(0));
    call(get("/api/v1/assignments"), otherTeacher).andExpect(jsonPath("$.length()").value(0));
    call(get("/api/v1/assignments"), s.student()).andExpect(jsonPath("$.length()").value(1));
    call(get("/api/v1/assignments"), s.teacher()).andExpect(jsonPath("$.length()").value(1));

    submit(s.student(), id, "Bài của tôi").andExpect(status().isOk());
    grade(otherTeacher, id, 5).andExpect(status().isNotFound());
    grade(s.student(), id, 5).andExpect(status().isForbidden());
    grade(s.admin(), id, 5).andExpect(status().isForbidden());
  }
}
