package com.englishplatform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.Role;
import com.englishplatform.identity.StudentAnonymized;
import com.englishplatform.learning.LearningEvents.SessionReportOverdue;
import com.englishplatform.learning.LearningEvents.SessionReportSubmitted;
import com.englishplatform.learning.internal.jobs.ReportOverdueJob;
import com.englishplatform.scheduling.SchedulingEvents.SessionFinished;
import com.englishplatform.scheduling.SessionStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

/** Phiếu đánh giá (FR-019): tạo khi buổi hoàn thành, nộp một lần, nhắc quá hạn, tiến bộ. */
@RecordApplicationEvents
class SessionReportFlowIT extends LearningFlowSupport {

  @Autowired ReportOverdueJob overdueJob;
  @Autowired ApplicationEvents events;
  @Autowired ApplicationEventPublisher publisher;

  private List<UUID> overdue(UUID sessionId) {
    return events.stream(SessionReportOverdue.class)
        .map(SessionReportOverdue::sessionId)
        .filter(sessionId::equals)
        .toList();
  }

  @Test
  void completedSessionCreatesOnePendingReportDueIn24Hours() throws Exception {
    Setup s = setup();
    UUID sessionId = completedWithPendingReport(s, 2);
    String endsAt = read(call(get("/api/v1/sessions/" + sessionId), s.teacher()), "$.endsAt");

    call(get("/api/v1/sessions/" + sessionId + "/report"), s.teacher())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(
            jsonPath("$.dueAt").value(Instant.parse(endsAt).plus(Duration.ofHours(24)).toString()));
    call(get("/api/v1/session-reports?status=PENDING"), s.teacher())
        .andExpect(jsonPath("$[?(@.sessionId == '" + sessionId + "')]").isNotEmpty());

    // phát lại event không tạo thêm phiếu (NFR-REL-11)
    new TransactionTemplate(txManager)
        .executeWithoutResult(
            tx ->
                publisher.publishEvent(
                    new SessionFinished(
                        sessionId,
                        s.enrollmentId(),
                        s.teacher().id(),
                        s.student().id(),
                        SessionStatus.COMPLETED,
                        Instant.parse(endsAt))));
    Thread.sleep(500);
    awaitReportCount(sessionId, 1);
  }

  @Test
  void otherOutcomesCreateNoReportAndCorrectionRemovesPendingOne() throws Exception {
    Setup s = setup();
    UUID noShow = finishedSession(s, 2, "STUDENT_NO_SHOW");
    Thread.sleep(500);
    awaitReportCount(noShow, 0);

    UUID completed = completedWithPendingReport(s, 3);
    call(
            post("/api/v1/sessions/" + completed + "/outcome"),
            s.admin(),
            "{\"outcome\":\"TEACHER_ABSENT\",\"note\":\"Chốt nhầm\"}")
        .andExpect(status().isOk());
    awaitReportCount(completed, 0);
  }

  @Test
  void submitValidatesRequiredFieldsAndTopics() throws Exception {
    Setup s = setup();
    List<String> topics = path(s);
    UUID sessionId = completedWithPendingReport(s, 2);

    submitReport(s.teacher(), sessionId, SCORES).andExpect(status().isBadRequest());
    submitReport(s.teacher(), sessionId, "\"scores\":{},\"comment\":\"Tốt\"")
        .andExpect(status().isBadRequest());
    submitReport(s.teacher(), sessionId, "\"scores\":{\"listening\":6},\"comment\":\"Tốt\"")
        .andExpect(status().isBadRequest());
    submitReport(s.teacher(), sessionId, "\"scores\":{\"listening\":0},\"comment\":\"Tốt\"")
        .andExpect(status().isBadRequest());
    submitReport(
            s.teacher(),
            sessionId,
            SCORES + ",\"comment\":\"Tốt\",\"taughtTopicIds\":[\"" + UUID.randomUUID() + "\"]")
        .andExpect(status().isBadRequest());
    submitReport(
            s.teacher(),
            sessionId,
            SCORES + ",\"comment\":\"Tốt\",\"assignments\":[{\"title\":\" \"}]")
        .andExpect(status().isBadRequest());
    // lỗi không để lại gì
    call(get("/api/v1/sessions/" + sessionId + "/report"), s.student())
        .andExpect(jsonPath("$.status").value("PENDING"));

    Instant due = clock.instant().plus(Duration.ofDays(3));
    submitReport(
            s.teacher(),
            sessionId,
            SCORES
                + ",\"comment\":\"Phát âm tiến bộ\",\"taughtTopicIds\":[\""
                + topics.get(0)
                + "\"],\"completedTopicIds\":[\""
                + topics.get(0)
                + "\"],\"assignments\":[{\"title\":\"Viết đoạn văn\",\"instructions\":\"100 từ\","
                + "\"dueAt\":\""
                + due
                + "\"},{\"title\":\"Đọc bài\"}]")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"))
        .andExpect(jsonPath("$.scores.listening").value(3))
        .andExpect(jsonPath("$.scores.reading").isEmpty())
        .andExpect(jsonPath("$.onTime").value(true));

    assertThat(
            events.stream(SessionReportSubmitted.class)
                .filter(e -> e.sessionId().equals(sessionId))
                .count())
        .isEqualTo(1);
    // học viên thấy nhận xét và bài tập
    call(get("/api/v1/sessions/" + sessionId + "/report"), s.student())
        .andExpect(jsonPath("$.comment").value("Phát âm tiến bộ"))
        .andExpect(jsonPath("$.taughtTopicIds[0]").value(topics.get(0)));
    call(get("/api/v1/assignments?studentId=" + s.student().id()), s.student())
        .andExpect(jsonPath("$.length()").value(2));
    // chủ đề hoàn thành
    call(get("/api/v1/students/" + s.student().id() + "/learning-path"), s.student())
        .andExpect(jsonPath("$.milestones[0].topics[0].status").value("DONE"))
        .andExpect(jsonPath("$.milestones[0].topics[1].status").value("PLANNED"));

    // nộp lại bị từ chối, không nhân đôi bài tập
    submitReport(s.teacher(), sessionId, SCORES + ",\"comment\":\"Lần hai\"")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
  }

  @Test
  void onlySessionTeacherSubmitsAndStrangersCannotRead() throws Exception {
    Setup s = setup();
    UUID sessionId = completedWithPendingReport(s, 2);
    String body = SCORES + ",\"comment\":\"Tốt\"";
    TestUser otherTeacher = user(Role.TEACHER);
    TestUser otherStudent = user(Role.STUDENT);

    submitReport(otherTeacher, sessionId, body).andExpect(status().isNotFound());
    submitReport(s.student(), sessionId, body).andExpect(status().isForbidden());
    submitReport(s.admin(), sessionId, body).andExpect(status().isForbidden());
    call(get("/api/v1/sessions/" + sessionId + "/report"), otherTeacher)
        .andExpect(status().isNotFound());
    call(get("/api/v1/sessions/" + sessionId + "/report"), otherStudent)
        .andExpect(status().isNotFound());
    call(get("/api/v1/session-reports"), s.student()).andExpect(status().isForbidden());
    call(get("/api/v1/session-reports?status=PENDING"), otherTeacher)
        .andExpect(jsonPath("$[?(@.sessionId == '" + sessionId + "')]").isEmpty());
    call(get("/api/v1/session-reports?status=PENDING"), s.admin())
        .andExpect(jsonPath("$[?(@.sessionId == '" + sessionId + "')]").isNotEmpty());
  }

  /** Đổi giáo viên sau buổi: giáo viên của buổi vẫn nộp phiếu của buổi mình đã dạy. */
  @Test
  void teacherOfSessionStillSubmitsAfterReassignment() throws Exception {
    Setup s = setup();
    UUID sessionId = completedWithPendingReport(s, 2);
    TestUser newTeacher = user(Role.TEACHER);
    call(
            put("/api/v1/enrollments/" + s.enrollmentId() + "/teacher"),
            s.admin(),
            "{\"teacherId\":\"" + newTeacher.id() + "\",\"reason\":\"GV nghỉ\"}")
        .andExpect(status().isOk());

    submitReport(newTeacher, sessionId, SCORES + ",\"comment\":\"Tốt\"")
        .andExpect(status().isNotFound());
    submitReport(s.teacher(), sessionId, SCORES + ",\"comment\":\"Tốt\"")
        .andExpect(status().isOk());
  }

  /** R1: giáo viên đã bị đổi chỉ còn việc nộp phiếu treo, không đọc được dữ liệu học viên nữa. */
  @Test
  void reassignedTeacherKeepsOnlyPendingDuty() throws Exception {
    Setup s = setup();
    UUID sessionId = completedWithPendingReport(s, 2);
    TestUser newTeacher = user(Role.TEACHER);
    call(
            put("/api/v1/enrollments/" + s.enrollmentId() + "/teacher"),
            s.admin(),
            "{\"teacherId\":\"" + newTeacher.id() + "\",\"reason\":\"GV nghỉ\"}")
        .andExpect(status().isOk());
    String row = "$[?(@.sessionId == '" + sessionId + "')]";

    call(get("/api/v1/session-reports?status=PENDING"), s.teacher())
        .andExpect(jsonPath(row).isNotEmpty());
    call(get("/api/v1/sessions/" + sessionId + "/report"), s.teacher()).andExpect(status().isOk());
    submitReport(s.teacher(), sessionId, SCORES + ",\"comment\":\"Nhận xét\"")
        .andExpect(status().isOk());

    call(get("/api/v1/sessions/" + sessionId + "/report"), s.teacher())
        .andExpect(status().isNotFound());
    call(get("/api/v1/session-reports"), s.teacher()).andExpect(jsonPath(row).isEmpty());
    call(get("/api/v1/sessions/" + sessionId + "/report"), newTeacher)
        .andExpect(jsonPath("$.comment").value("Nhận xét"));
  }

  /**
   * T148 (U6 R5): phiếu cho biết giáo viên của buổi còn phụ trách không; khi không còn (đổi giáo
   * viên, giáo viên nghỉ việc) giáo vụ đóng được phiếu treo kèm lý do, hết nhắc quá hạn.
   */
  @Test
  void adminClosesPendingReportOfFormerTeacher() throws Exception {
    Setup s = setup();
    UUID sessionId = completedWithPendingReport(s, 2);
    String report = "/api/v1/sessions/" + sessionId + "/report";
    String close = report + "/close";
    String row = "$[?(@.sessionId == '" + sessionId + "')]";

    call(get(report), s.teacher()).andExpect(jsonPath("$.teacherAssigned").value(true));
    call(post(close), s.admin(), "{\"reason\":\"Thử\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

    TestUser newTeacher = user(Role.TEACHER);
    call(
            put("/api/v1/enrollments/" + s.enrollmentId() + "/teacher"),
            s.admin(),
            "{\"teacherId\":\"" + newTeacher.id() + "\",\"reason\":\"GV nghỉ\"}")
        .andExpect(status().isOk());
    call(get(report), s.teacher()).andExpect(jsonPath("$.teacherAssigned").value(false));
    call(get(report), s.admin()).andExpect(jsonPath("$.teacherAssigned").value(false));

    call(post(close), s.teacher(), "{\"reason\":\"Thử\"}").andExpect(status().isForbidden());
    call(post(close), newTeacher, "{\"reason\":\"Thử\"}").andExpect(status().isForbidden());
    call(post(close), s.admin(), "{\"reason\":\" \"}").andExpect(status().isBadRequest());
    call(post(close), s.admin(), "{\"reason\":\"Giáo viên nghỉ việc, không nộp được\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.closeReason").value("Giáo viên nghỉ việc, không nộp được"))
        .andExpect(jsonPath("$.closedAt").isNotEmpty());

    call(post(close), s.admin(), "{\"reason\":\"Lần hai\"}").andExpect(status().isConflict());
    submitReport(s.teacher(), sessionId, SCORES + ",\"comment\":\"Muộn\"")
        .andExpect(status().isConflict());
    call(get("/api/v1/session-reports?status=PENDING"), s.admin())
        .andExpect(jsonPath(row).isEmpty());
    call(get("/api/v1/session-reports"), s.teacher()).andExpect(jsonPath(row).isEmpty());
    call(get(report), newTeacher).andExpect(jsonPath("$.status").value("CLOSED"));

    clock.advance(Duration.ofHours(25));
    overdueJob.run();
    assertThat(overdue(sessionId)).isEmpty();
  }

  /** R2: event tới sai thứ tự không quyết định phiếu; trạng thái thật của buổi mới là căn cứ. */
  @Test
  void staleEventsFollowActualSessionStatus() throws Exception {
    Setup s = setup();
    UUID completed = completedWithPendingReport(s, 2);
    UUID noShow = finishedSession(s, 2, "STUDENT_NO_SHOW");
    Instant endsAt = clock.instant();
    publishFinished(s, completed, SessionStatus.STUDENT_NO_SHOW, endsAt);
    publishFinished(s, noShow, SessionStatus.COMPLETED, endsAt);
    Thread.sleep(800);

    awaitReportCount(completed, 1);
    awaitReportCount(noShow, 0);
  }

  private void publishFinished(Setup s, UUID sessionId, SessionStatus outcome, Instant endsAt) {
    new TransactionTemplate(txManager)
        .executeWithoutResult(
            tx ->
                publisher.publishEvent(
                    new SessionFinished(
                        sessionId,
                        s.enrollmentId(),
                        s.teacher().id(),
                        s.student().id(),
                        outcome,
                        endsAt)));
  }

  /** F5: buổi đã nộp phiếu rồi được sửa thành không hoàn thành thì không vào biểu đồ. */
  @Test
  void correctedSessionLeavesProgressButKeepsReport() throws Exception {
    Setup s = setup();
    UUID sessionId = completedWithPendingReport(s, 2);
    submitReport(s.teacher(), sessionId, SCORES + ",\"comment\":\"Tốt\"")
        .andExpect(status().isOk());
    String path = "/api/v1/students/" + s.student().id() + "/progress";
    call(get(path), s.student()).andExpect(jsonPath("$.skillSeries.length()").value(1));

    call(
            post("/api/v1/sessions/" + sessionId + "/outcome"),
            s.admin(),
            "{\"outcome\":\"STUDENT_NO_SHOW\",\"note\":\"Chốt nhầm\"}")
        .andExpect(status().isOk());

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                call(get(path), s.student())
                    .andExpect(jsonPath("$.skillSeries.length()").value(0))
                    .andExpect(jsonPath("$.recentReports.length()").value(0)));
    awaitReportCount(sessionId, 1);

    call(
            post("/api/v1/sessions/" + sessionId + "/outcome"),
            s.admin(),
            "{\"outcome\":\"COMPLETED\",\"note\":\"Sửa lại\"}")
        .andExpect(status().isOk());
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                call(get(path), s.student())
                    .andExpect(jsonPath("$.skillSeries.length()").value(1)));
  }

  /** R9: chủ đề hoàn thành cũng là chủ đề đã dạy; không lặp. */
  @Test
  void completedTopicsCountAsTaughtWithoutDuplicates() throws Exception {
    Setup s = setup();
    List<String> topics = path(s);
    UUID sessionId = completedWithPendingReport(s, 2);
    submitReport(
            s.teacher(),
            sessionId,
            SCORES
                + ",\"comment\":\"Tốt\",\"taughtTopicIds\":[\""
                + topics.get(0)
                + "\",\""
                + topics.get(0)
                + "\"],\"completedTopicIds\":[\""
                + topics.get(1)
                + "\"]")
        .andExpect(jsonPath("$.taughtTopicIds.length()").value(2))
        .andExpect(jsonPath("$.taughtTopicIds[1]").value(topics.get(1)));
  }

  /** R14: giáo viên đã bị đổi chỉ nộp được nhận xét, không đổi được lộ trình của học viên. */
  @Test
  void reassignedTeacherCannotTouchTopics() throws Exception {
    Setup s = setup();
    List<String> topics = path(s);
    UUID sessionId = completedWithPendingReport(s, 2);
    call(
            put("/api/v1/enrollments/" + s.enrollmentId() + "/teacher"),
            s.admin(),
            "{\"teacherId\":\"" + user(Role.TEACHER).id() + "\",\"reason\":\"GV nghỉ\"}")
        .andExpect(status().isOk());

    submitReport(
            s.teacher(),
            sessionId,
            SCORES + ",\"comment\":\"Tốt\",\"completedTopicIds\":[\"" + topics.get(0) + "\"]")
        .andExpect(status().isBadRequest());
    call(get("/api/v1/students/" + s.student().id() + "/learning-path"), s.admin())
        .andExpect(jsonPath("$.milestones[0].topics[0].status").value("PLANNED"));
    submitReport(s.teacher(), sessionId, SCORES + ",\"comment\":\"Tốt\"")
        .andExpect(status().isOk());
  }

  /** R16: danh sách chủ đề có null khi chưa có lộ trình là dữ liệu sai, không phải lỗi máy chủ. */
  @Test
  void nullTopicIsRejectedWithoutPath() throws Exception {
    Setup s = setup();
    UUID sessionId = completedWithPendingReport(s, 2);
    submitReport(s.teacher(), sessionId, SCORES + ",\"comment\":\"Tốt\",\"taughtTopicIds\":[null]")
        .andExpect(status().isBadRequest());
  }

  /** R4: phần trăm hoàn thành làm tròn giống trang lộ trình (2/3 là 67). */
  @Test
  void pathCompletionRoundsLikeLearningPath() throws Exception {
    Setup s = setup();
    call(
            post("/api/v1/students/" + s.student().id() + "/learning-path"),
            s.teacher(),
            "{\"title\":\"Lộ trình\",\"milestones\":[{\"title\":\"Chặng\",\"topics\":"
                + "[{\"title\":\"A\"},{\"title\":\"B\"},{\"title\":\"C\"}]}]}")
        .andExpect(status().isCreated());
    String pathBody =
        call(get("/api/v1/students/" + s.student().id() + "/learning-path"), s.teacher())
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<String> ids = com.jayway.jsonpath.JsonPath.read(pathBody, "$.milestones[0].topics[*].id");
    UUID sessionId = completedWithPendingReport(s, 2);
    submitReport(
            s.teacher(),
            sessionId,
            SCORES
                + ",\"comment\":\"Tốt\",\"completedTopicIds\":[\""
                + ids.get(0)
                + "\",\""
                + ids.get(1)
                + "\"]")
        .andExpect(status().isOk());

    call(get("/api/v1/students/" + s.student().id() + "/progress"), s.student())
        .andExpect(jsonPath("$.pathCompletion").value(67));
  }

  /** F11: nhận xét trong phiếu có giới hạn độ dài. */
  @Test
  void oversizedCommentIsRejected() throws Exception {
    Setup s = setup();
    UUID sessionId = completedWithPendingReport(s, 2);
    submitReport(s.teacher(), sessionId, SCORES + ",\"comment\":\"" + "x".repeat(5001) + "\"")
        .andExpect(status().isBadRequest());
    submitReport(
            s.teacher(),
            sessionId,
            SCORES
                + ",\"comment\":\"Tốt\",\"assignments\":[{\"title\":\"B\",\"instructions\":\""
                + "x".repeat(5001)
                + "\"}]")
        .andExpect(status().isBadRequest());
  }

  @Test
  void nullAssignmentIsRejected() throws Exception {
    Setup s = setup();
    UUID sessionId = completedWithPendingReport(s, 2);
    submitReport(s.teacher(), sessionId, SCORES + ",\"comment\":\"Tốt\",\"assignments\":[null]")
        .andExpect(status().isBadRequest());
  }

  @Test
  void overdueReminderFiresOnceAndSkipsSubmitted() throws Exception {
    Setup s = setup();
    // buổi nộp phiếu trước: tua clock tới buổi sau không làm phiếu chưa nộp quá hạn sẵn
    UUID submitted = completedWithPendingReport(s, 2);
    UUID pending = completedWithPendingReport(s, 3);
    submitReport(s.teacher(), submitted, SCORES + ",\"comment\":\"Tốt\"")
        .andExpect(status().isOk());

    overdueJob.run();
    assertThat(overdue(pending)).isEmpty();

    clock.advance(Duration.ofHours(25));
    overdueJob.run();
    overdueJob.run();

    assertThat(overdue(pending)).hasSize(1);
    assertThat(overdue(submitted)).isEmpty();
    call(get("/api/v1/session-reports?status=PENDING"), s.admin())
        .andExpect(jsonPath("$[?(@.sessionId == '" + pending + "')].overdue").value(true));

    // nộp muộn vẫn được nhận và không tính đúng hạn
    submitReport(s.teacher(), pending, SCORES + ",\"comment\":\"Muộn\"")
        .andExpect(jsonPath("$.onTime").value(false));
  }

  @Test
  void anonymizedStudentHasNoReportWorkLeft() throws Exception {
    Setup s = setup();
    UUID pending = completedWithPendingReport(s, 2);

    new TransactionTemplate(txManager)
        .executeWithoutResult(
            tx ->
                publisher.publishEvent(new StudentAnonymized(s.student().id(), UUID.randomUUID())));
    Thread.sleep(800);

    clock.advance(Duration.ofHours(25));
    overdueJob.run();
    assertThat(overdue(pending)).as("phiếu còn treo của HV ẩn danh không nhắc GV").isEmpty();
  }

  @Test
  void progressShowsSeriesCompletionAndRecentReports() throws Exception {
    Setup s = setup();
    List<String> topics = path(s);
    for (int i = 0; i < 3; i++) {
      UUID sessionId = completedWithPendingReport(s, 2);
      submitReport(
              s.teacher(),
              sessionId,
              "\"scores\":{\"listening\":"
                  + (i + 2)
                  + "},\"comment\":\"Buổi "
                  + i
                  + "\""
                  + (i == 0 ? ",\"completedTopicIds\":[\"" + topics.get(0) + "\"]" : ""))
          .andExpect(status().isOk());
    }
    // phiếu chưa nộp không vào biểu đồ
    completedWithPendingReport(s, 2);
    String path = "/api/v1/students/" + s.student().id() + "/progress";

    call(get(path), s.student())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.skillSeries.length()").value(3))
        .andExpect(jsonPath("$.skillSeries[0].listening").value(2))
        .andExpect(jsonPath("$.skillSeries[2].listening").value(4))
        .andExpect(jsonPath("$.skillSeries[0].speaking").isEmpty())
        .andExpect(jsonPath("$.pathCompletion").value(50))
        .andExpect(jsonPath("$.recentReports.length()").value(3))
        .andExpect(jsonPath("$.recentReports[0].comment").value("Buổi 2"));
    call(get(path), s.teacher()).andExpect(status().isOk());
    call(get(path), s.admin()).andExpect(status().isOk());
    call(get(path), user(Role.TEACHER)).andExpect(status().isNotFound());
    call(get(path), user(Role.STUDENT)).andExpect(status().isNotFound());
  }

  @Test
  void progressWithoutPathOrReportsIsEmpty() throws Exception {
    Setup s = setup();
    call(get("/api/v1/students/" + s.student().id() + "/progress"), s.student())
        .andExpect(jsonPath("$.skillSeries.length()").value(0))
        .andExpect(jsonPath("$.pathCompletion").isEmpty())
        .andExpect(jsonPath("$.recentReports.length()").value(0));
  }
}
