package com.englishplatform.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Chuyển từ verify U3: ngày nghỉ chen đặt buổi (R6), giới hạn {@code GET /sessions} (R8). */
class SchedulingHardeningIT extends SchedulingTestSupport {

  /** Không bao giờ có buổi đã đặt nằm trong ngày nghỉ, dù hai thao tác chạy cùng lúc (FR-008). */
  @Test
  void bookingAndDayOffNeverBothSucceed() throws Exception {
    Setup s = setup(12);
    for (int round = 0; round < 10; round++) {
      int day = 2 + round;
      Instant startsAt = at(day, 10, 0);
      LocalDate date = LocalDate.now(VN).plusDays(day);
      CountDownLatch start = new CountDownLatch(1);
      AtomicInteger booked = new AtomicInteger();
      AtomicInteger dayOff = new AtomicInteger();

      Thread booking =
          concurrently(
              start,
              booked,
              () ->
                  book(s.student(), s.enrollmentId(), startsAt)
                      .andReturn()
                      .getResponse()
                      .getStatus(),
              201);
      Thread exception =
          concurrently(
              start,
              dayOff,
              () ->
                  call(
                          post("/api/v1/teachers/" + s.teacher().id() + "/availability/exceptions"),
                          s.teacher(),
                          "{\"date\":\"" + date + "\",\"reason\":\"Nghỉ\"}")
                      .andReturn()
                      .getResponse()
                      .getStatus(),
              201);
      start.countDown();
      booking.join();
      exception.join();

      assertThat(booked.get() + dayOff.get()).as("ngày %s", date).isEqualTo(1);
    }
  }

  private interface StatusCall {
    int run() throws Exception;
  }

  private static Thread concurrently(
      CountDownLatch start, AtomicInteger success, StatusCall action, int expected) {
    Thread t =
        new Thread(
            () -> {
              try {
                start.await();
                if (action.run() == expected) {
                  success.incrementAndGet();
                }
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
            });
    t.start();
    return t;
  }

  @Test
  void sessionListIsPagedAndBounded() throws Exception {
    Setup s = setup(4);
    bookOk(s.student(), s.enrollmentId(), at(2, 10, 0));
    bookOk(s.student(), s.enrollmentId(), at(3, 10, 0));
    bookOk(s.student(), s.enrollmentId(), at(4, 10, 0));

    call(get("/api/v1/sessions"), s.student()).andExpect(jsonPath("$.length()").value(3));
    call(get("/api/v1/sessions?size=2"), s.student())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].startsAt").value(at(2, 10, 0).toString()));
    call(get("/api/v1/sessions?size=2&page=1"), s.student())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].startsAt").value(at(4, 10, 0).toString()));
    call(get("/api/v1/sessions?size=501"), s.student()).andExpect(status().isBadRequest());
    call(get("/api/v1/sessions?size=0"), s.student()).andExpect(status().isBadRequest());
    call(get("/api/v1/sessions?page=-1"), s.student()).andExpect(status().isBadRequest());
    call(get("/api/v1/sessions?page=2147483647"), s.student()).andExpect(status().isBadRequest());
  }

  /** F4: danh sách buổi chưa chốt cũng có giới hạn. */
  @Test
  void outcomePendingListIsPaged() throws Exception {
    Setup s = setup(4);
    Instant first = at(2, 10, 0);
    bookOk(s.student(), s.enrollmentId(), first);
    bookOk(s.student(), s.enrollmentId(), at(2, 12, 0));
    clock.set(at(2, 14, 0));

    call(get("/api/v1/sessions?outcomePending=true"), s.teacher())
        .andExpect(jsonPath("$.length()").value(2));
    call(get("/api/v1/sessions?outcomePending=true&size=1"), s.teacher())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].startsAt").value(first.toString()));
  }
}
