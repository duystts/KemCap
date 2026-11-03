package com.englishplatform.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.Role;
import com.englishplatform.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Báo cáo tài chính (FR-025, FR-027, NFR-REL-03): đã thu theo kỳ, công nợ là số dư hiện tại, ghi
 * danh đã hủy không tính vào công nợ, thu vượt không bù cho ghi danh khác.
 */
@RecordApplicationEvents
class BillingReportIT extends ApiTestSupport {

  private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final long PRICE = 1_000_000;

  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired ApplicationEvents events;

  private UUID enroll(TestUser admin, TestUser teacher, TestUser student) {
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói", 6, 45, 90, PRICE));
    return enrollmentService
        .create(student.id(), pkg.getId(), teacher.id(), null, admin.id())
        .getId();
  }

  private void pay(TestUser admin, UUID enrollmentId, long amount, LocalDate paidAt)
      throws Exception {
    call(
            post("/api/v1/enrollments/" + enrollmentId + "/payments"),
            admin,
            "{\"amountVnd\":" + amount + ",\"paidAt\":\"" + paidAt + "\",\"method\":\"CASH\"}")
        .andExpect(status().isCreated());
  }

  private long revenue(TestUser admin, String field, LocalDate from, LocalDate to)
      throws Exception {
    return Long.parseLong(
        read(
            call(get("/api/v1/reports/revenue?from=" + from + "&to=" + to), admin)
                .andExpect(status().isOk()),
            "$." + field));
  }

  private List<String> debtors(TestUser admin) throws Exception {
    String body =
        call(get("/api/v1/receivables?hasDebt=true"), admin)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$[*].enrollmentId");
  }

  @Test
  void collectedFollowsPaidAtRangeAndAdjustments() throws Exception {
    TestUser admin = user(Role.ADMIN);
    UUID enrollmentId = enroll(admin, user(Role.TEACHER), user(Role.STUDENT));
    LocalDate today = LocalDate.now(VN);
    // ngày riêng của test: DB dùng chung với các IT khác
    LocalDate past = LocalDate.of(2001, 1, 1).plusDays(UUID.randomUUID().hashCode() & 0xFFF);

    long todayBefore = revenue(admin, "collectedVnd", today, today);
    pay(admin, enrollmentId, 300_000, past);
    pay(admin, enrollmentId, 400_000, today);
    call(
            post("/api/v1/enrollments/" + enrollmentId + "/payments/adjustments"),
            admin,
            "{\"amountVnd\":-50000,\"note\":\"Nhập nhầm\"}")
        .andExpect(status().isCreated());

    assertThat(revenue(admin, "collectedVnd", past, past)).isEqualTo(300_000);
    assertThat(revenue(admin, "collectedVnd", past.minusDays(1), past.minusDays(1))).isZero();
    assertThat(revenue(admin, "collectedVnd", today, today)).isEqualTo(todayBefore + 350_000);

    // công nợ = học phí − tổng đã thu kể cả điều chỉnh; khớp sao kê của ghi danh
    call(get("/api/v1/enrollments/" + enrollmentId + "/payments"), admin)
        .andExpect(jsonPath("$.amountPaidVnd").value(650_000))
        .andExpect(jsonPath("$.debtVnd").value(350_000));
    call(get("/api/v1/receivables?hasDebt=true"), admin)
        .andExpect(
            jsonPath("$[?(@.enrollmentId == '" + enrollmentId + "')].debtVnd").value(350_000));
  }

  @Test
  void debtIsCurrentBalanceRegardlessOfRange() throws Exception {
    TestUser admin = user(Role.ADMIN);
    LocalDate today = LocalDate.now(VN);
    LocalDate longAgo = LocalDate.of(2001, 1, 1);

    long before = revenue(admin, "debtVnd", today, today);
    UUID enrollmentId = enroll(admin, user(Role.TEACHER), user(Role.STUDENT));

    assertThat(revenue(admin, "debtVnd", today, today)).isEqualTo(before + PRICE);
    assertThat(revenue(admin, "debtVnd", longAgo, longAgo)).isEqualTo(before + PRICE);
    assertThat(debtors(admin)).contains(enrollmentId.toString());
  }

  @Test
  void overpaymentDoesNotOffsetOtherDebt() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    LocalDate today = LocalDate.now(VN);
    long before = revenue(admin, "debtVnd", today, today);

    UUID overpaid = enroll(admin, teacher, user(Role.STUDENT));
    UUID owing = enroll(admin, teacher, user(Role.STUDENT));
    pay(admin, overpaid, PRICE + 200_000, today);

    assertThat(revenue(admin, "debtVnd", today, today)).isEqualTo(before + PRICE);
    assertThat(debtors(admin)).contains(owing.toString()).doesNotContain(overpaid.toString());
  }

  @Test
  void cancelledEnrollmentLeavesDebtTotalsButKeepsRecords() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser student = user(Role.STUDENT);
    LocalDate today = LocalDate.now(VN);
    UUID enrollmentId = enroll(admin, user(Role.TEACHER), student);
    pay(admin, enrollmentId, 400_000, today);
    long debtBefore = revenue(admin, "debtVnd", today, today);
    long collectedBefore = revenue(admin, "collectedVnd", today, today);

    // R7 (U2): lưu ai hủy, lúc nào, vì sao
    call(
            post("/api/v1/enrollments/" + enrollmentId + "/cancel"),
            admin,
            "{\"reason\":\"Chuyển nơi học\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.cancelReason").value("Chuyển nơi học"))
        .andExpect(jsonPath("$.cancelledBy").value(admin.id().toString()))
        .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

    assertThat(revenue(admin, "debtVnd", today, today)).isEqualTo(debtBefore - 600_000);
    assertThat(revenue(admin, "collectedVnd", today, today)).isEqualTo(collectedBefore);
    assertThat(debtors(admin)).doesNotContain(enrollmentId.toString());

    // bản ghi còn nguyên trong danh sách đầy đủ, có cờ đã hủy
    call(get("/api/v1/receivables"), admin)
        .andExpect(jsonPath("$[?(@.enrollmentId == '" + enrollmentId + "')].cancelled").value(true))
        .andExpect(
            jsonPath("$[?(@.enrollmentId == '" + enrollmentId + "')].debtVnd").value(600_000));
    call(get("/api/v1/enrollments/" + enrollmentId + "/payments"), admin)
        .andExpect(jsonPath("$.amountPaidVnd").value(400_000));

    // R5 (U2): vẫn sửa được khoản nhập nhầm, nhưng không thu thêm trên ghi danh đã hủy
    call(
            post("/api/v1/enrollments/" + enrollmentId + "/payments/adjustments"),
            admin,
            "{\"amountVnd\":-100000,\"note\":\"Nhập nhầm\"}")
        .andExpect(status().isCreated());
    call(
            post("/api/v1/enrollments/" + enrollmentId + "/payments"),
            admin,
            "{\"amountVnd\":1000,\"paidAt\":\"" + today + "\",\"method\":\"CASH\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_ACTIVE"));
    assertThat(revenue(admin, "collectedVnd", today, today)).isEqualTo(collectedBefore - 100_000);
  }

  /** R6 (U2): hai thanh toán đầu đồng thời: tổng đúng, chỉ một lượt là thanh toán đầu tiên. */
  @Test
  void concurrentFirstPaymentsKeepTotalsAndOneFirst() throws Exception {
    TestUser admin = user(Role.ADMIN);
    UUID enrollmentId = enroll(admin, user(Role.TEACHER), user(Role.STUDENT));
    LocalDate today = LocalDate.now(VN);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger created = new AtomicInteger();
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Thread t =
          new Thread(
              () -> {
                try {
                  start.await();
                  int code =
                      call(
                              post("/api/v1/enrollments/" + enrollmentId + "/payments"),
                              admin,
                              "{\"amountVnd\":100000,\"paidAt\":\""
                                  + today
                                  + "\",\"method\":\"CASH\"}")
                          .andReturn()
                          .getResponse()
                          .getStatus();
                  if (code == 201) {
                    created.incrementAndGet();
                  }
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

    assertThat(created.get()).isEqualTo(2);
    call(get("/api/v1/enrollments/" + enrollmentId + "/payments"), admin)
        .andExpect(jsonPath("$.amountPaidVnd").value(200_000))
        .andExpect(jsonPath("$.debtVnd").value(PRICE - 200_000))
        .andExpect(jsonPath("$.payments.length()").value(2));
    assertThat(
            events.stream(PaymentRecorded.class)
                .filter(e -> e.enrollmentId().equals(enrollmentId))
                .filter(PaymentRecorded::isFirstPayment)
                .count())
        .isEqualTo(1);
  }

  @Test
  void onlyAdminReadsFinancialReportsAndRangeIsValidated() throws Exception {
    TestUser admin = user(Role.ADMIN);
    for (TestUser other : new TestUser[] {user(Role.TEACHER), user(Role.STUDENT)}) {
      call(get("/api/v1/receivables"), other).andExpect(status().isForbidden());
      call(get("/api/v1/reports/revenue"), other).andExpect(status().isForbidden());
    }
    call(get("/api/v1/reports/revenue?from=2026-02-01&to=2026-01-01"), admin)
        .andExpect(status().isBadRequest());
    call(get("/api/v1/reports/revenue"), admin).andExpect(status().isOk());
  }
}
