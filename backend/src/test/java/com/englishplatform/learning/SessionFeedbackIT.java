package com.englishplatform.learning;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.Role;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Đánh giá sao (FR-022): buổi hoàn thành của chính học viên, 7 ngày, một lần; chỉ giáo vụ xem. */
class SessionFeedbackIT extends LearningFlowSupport {

  private ResultActions rate(TestUser as, UUID sessionId, String body) throws Exception {
    return call(post("/api/v1/sessions/" + sessionId + "/feedback"), as, body);
  }

  @Test
  void studentRatesOnceAndOnlyAdminReads() throws Exception {
    Setup s = setup();
    UUID sessionId = finishedSession(s, 2, "COMPLETED");

    rate(s.student(), sessionId, "{\"stars\":0}").andExpect(status().isBadRequest());
    rate(s.student(), sessionId, "{\"stars\":6}").andExpect(status().isBadRequest());
    rate(s.student(), sessionId, "{\"stars\":4}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.stars").value(4));
    rate(s.student(), sessionId, "{\"stars\":5}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FEEDBACK_EXISTS"));

    call(get("/api/v1/sessions/" + sessionId + "/feedback"), s.admin())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stars").value(4))
        .andExpect(jsonPath("$.comment").isEmpty());
    call(get("/api/v1/sessions/" + sessionId + "/feedback"), s.teacher())
        .andExpect(status().isForbidden());
    call(get("/api/v1/sessions/" + sessionId + "/feedback"), s.student())
        .andExpect(status().isForbidden());
  }

  /** F7: học viên biết buổi nào đã chấm để FE ẩn lời mời; không lộ của người khác. */
  @Test
  void studentSeesOwnFeedbackOnly() throws Exception {
    Setup s = setup();
    UUID sessionId = finishedSession(s, 2, "COMPLETED");
    rate(s.student(), sessionId, "{\"stars\":4}").andExpect(status().isCreated());

    call(get("/api/v1/session-feedback/mine"), s.student())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].sessionId").value(sessionId.toString()))
        .andExpect(jsonPath("$[0].stars").value(4));
    call(get("/api/v1/session-feedback/mine"), user(Role.STUDENT))
        .andExpect(jsonPath("$.length()").value(0));
    call(get("/api/v1/session-feedback/mine"), s.teacher()).andExpect(status().isForbidden());
  }

  @Test
  void onlyWithinSevenDaysOfSessionEnd() throws Exception {
    Setup s = setup();
    UUID inTime = finishedSession(s, 2, "COMPLETED");
    UUID late = finishedSession(s, 2, "COMPLETED");

    // buổi sau kết thúc muộn hơn buổi đầu 2 ngày: tiến tới mốc 6 ngày của buổi đầu
    clock.advance(Duration.ofDays(4));
    rate(s.student(), inTime, "{\"stars\":5,\"comment\":\"Hay\"}").andExpect(status().isCreated());
    // đã tới mốc 8 ngày của buổi đó
    clock.advance(Duration.ofDays(4));
    rate(s.student(), late, "{\"stars\":5}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FEEDBACK_WINDOW_CLOSED"));
  }

  @Test
  void onlyCompletedSessionOfThatStudent() throws Exception {
    Setup s = setup();
    UUID noShow = finishedSession(s, 2, "STUDENT_NO_SHOW");
    UUID completed = finishedSession(s, 2, "COMPLETED");

    rate(s.student(), noShow, "{\"stars\":3}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    rate(user(Role.STUDENT), completed, "{\"stars\":3}").andExpect(status().isNotFound());
    rate(s.teacher(), completed, "{\"stars\":3}").andExpect(status().isForbidden());
    rate(s.admin(), completed, "{\"stars\":3}").andExpect(status().isForbidden());
    rate(s.student(), UUID.randomUUID(), "{\"stars\":3}").andExpect(status().isNotFound());
    call(get("/api/v1/sessions/" + completed + "/feedback"), s.admin())
        .andExpect(status().isNotFound());
  }
}
