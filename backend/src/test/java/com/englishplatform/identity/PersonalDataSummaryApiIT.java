package com.englishplatform.identity;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.notification.internal.Notification;
import com.englishplatform.notification.internal.NotificationRepository;
import com.englishplatform.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** API đọc cho bản tóm tắt dữ liệu cá nhân (FR-031): chỉ giáo vụ, đúng người được hỏi. */
class PersonalDataSummaryApiIT extends ApiTestSupport {

  @Autowired NotificationRepository notifications;

  @Test
  void adminReadsPolicyConsentsOfStudent() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);

    call(get("/api/v1/users/" + student.id() + "/policy-consents"), admin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].policyVersion").value(privacy.policyVersion()))
        .andExpect(jsonPath("$[0].acceptedAt").isNotEmpty());
  }

  @Test
  void policyConsentsOfUnknownUserIsNotFound() throws Exception {
    TestUser admin = user(Role.ADMIN);

    call(get("/api/v1/users/" + java.util.UUID.randomUUID() + "/policy-consents"), admin)
        .andExpect(status().isNotFound());
  }

  @Test
  void adminReadsAllNotificationsOfOneUserOnly() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    TestUser other = user(Role.STUDENT);
    // Quá kích thước trang của /notifications để chắc chắn lấy đủ, không bị cắt
    for (int i = 0; i < 60; i++) {
      notifications.save(new Notification(student.id(), "TEST", "Của học viên " + i, "x", null));
    }
    notifications.save(new Notification(other.id(), "TEST", "Của người khác", "x", null));

    call(get("/api/v1/users/" + student.id() + "/notifications"), admin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(60)));
  }

  @Test
  void onlyAdminCanRead() throws Exception {
    TestUser student = user(Role.STUDENT);
    TestUser teacher = user(Role.TEACHER);

    for (TestUser as : new TestUser[] {student, teacher}) {
      call(get("/api/v1/users/" + student.id() + "/notifications"), as)
          .andExpect(status().isForbidden());
      call(get("/api/v1/users/" + student.id() + "/policy-consents"), as)
          .andExpect(status().isForbidden());
    }
  }
}
