package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.identity.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** 0 trùng lịch dưới tranh chấp (NFR-REL-01, SC-003): exclusion constraint + cùng transaction. */
class BookingConcurrencyIT extends SchedulingTestSupport {

  @Test
  void tenStudentsRaceForOneTeacherSlotOnlyOneWins() throws Exception {
    Setup base = setup(5);
    List<Setup> students = new ArrayList<>(List.of(base));
    for (int i = 0; i < 9; i++) {
      students.add(anotherStudent(base, 5));
    }
    Instant slot = at(3, 10, 0);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(10);
    List<Future<Integer>> results = new ArrayList<>();
    try {
      for (Setup s : students) {
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  return book(s.student(), s.enrollmentId(), slot)
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      }
      start.countDown();
      List<Integer> statuses = new ArrayList<>();
      for (Future<Integer> f : results) {
        statuses.add(f.get(60, TimeUnit.SECONDS));
      }
      assertThat(statuses.stream().filter(c -> c == 201).count()).isEqualTo(1);
      assertThat(statuses.stream().filter(c -> c == 409).count()).isEqualTo(9);
    } finally {
      pool.shutdownNow();
    }
    // bên thua không bị giữ chỗ (transaction rollback cả reserve)
    long reserved =
        students.stream().mapToInt(s -> enrollment(s.enrollmentId()).sessionsReserved()).sum();
    assertThat(reserved).isEqualTo(1);
  }

  @Test
  void studentCannotHoldOverlappingSessionsWithTwoTeachers() throws Exception {
    Setup first = setup(5);
    TestUser otherTeacher = user(Role.TEACHER);
    availableAllWeek(otherTeacher.id());
    var secondEnrollment = enroll(first.student().id(), otherTeacher.id(), first.admin().id(), 5);

    book(first.student(), first.enrollmentId(), at(4, 10, 0)).andExpect(status().isCreated());
    book(first.student(), secondEnrollment, at(4, 10, 30))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SLOT_TAKEN"));
    assertThat(enrollment(secondEnrollment).sessionsReserved()).isZero();
    assertThat(enrollment(first.enrollmentId()).sessionsReserved()).isEqualTo(1);
  }

  @Test
  void bookedSlotDisappearsFromSlotListAndSlotRulesAreEnforced() throws Exception {
    Setup s = setup(5);
    Instant slot = at(5, 9, 0);
    book(s.student(), s.enrollmentId(), slot).andExpect(status().isCreated());
    String slots =
        call(
                get(
                    "/api/v1/enrollments/"
                        + s.enrollmentId()
                        + "/slots?from="
                        + at(5, 0, 0)
                        + "&to="
                        + at(6, 0, 0)),
                s.student())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(slots).doesNotContain(slot.toString()).contains(at(5, 10, 0).toString());

    // ngoài lịch rảnh (05:00), lệch bước 30 phút, quá 30 ngày
    book(s.student(), s.enrollmentId(), at(6, 5, 0)).andExpect(status().isBadRequest());
    book(s.student(), s.enrollmentId(), at(6, 10, 10)).andExpect(status().isBadRequest());
    book(s.student(), s.enrollmentId(), at(31, 10, 0)).andExpect(status().isBadRequest());
    // học viên khác không đặt được trên ghi danh này
    Setup other = anotherStudent(s, 2);
    book(other.student(), s.enrollmentId(), at(6, 10, 0)).andExpect(status().isNotFound());
  }

  @Test
  void pendingPaymentEnrollmentCannotBook() throws Exception {
    Setup s = setup(5);
    var courseId =
        courseService.createCourse("Khóa chờ " + java.util.UUID.randomUUID(), null).getId();
    var pkg =
        courseService.createPackage(
            courseId,
            new com.englishplatform.catalog.internal.course.CourseService.PackageSpec(
                "Gói", 4, 45, 30, 0));
    var pending =
        enrollmentService.create(
            s.student().id(), pkg.getId(), s.teacher().id(), null, s.admin().id());
    book(s.student(), pending.getId(), at(3, 15, 0))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_ACTIVE"));
  }
}
