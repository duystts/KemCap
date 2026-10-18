package com.englishplatform.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.billing.internal.Payment;
import com.englishplatform.billing.internal.PaymentService;
import com.englishplatform.billing.internal.ReceivableRepository;
import com.englishplatform.catalog.CatalogEvents.EnrollmentActivated;
import com.englishplatform.catalog.CatalogEvents.EnrollmentCreated;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.Enrollment;
import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.Role;
import com.englishplatform.support.ApiTestSupport;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.EnableScenarios;
import org.springframework.modulith.test.Scenario;

/**
 * Luồng catalog ↔ billing qua event (Modulith {@link Scenario}): ghi danh → công nợ; thanh toán đầu
 * → kích hoạt; thanh toán sau không đổi hạn; học viên chỉ đọc thanh toán của mình.
 */
@EnableScenarios
class BillingFlowTest extends ApiTestSupport {

  static final long PRICE = 3_000_000;

  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired EnrollmentRepository enrollments;
  @Autowired PaymentService paymentService;
  @Autowired ReceivableRepository receivables;

  @Test
  void enrollmentCreatedOpensReceivableOnce(Scenario scenario) {
    UUID enrollmentId = UUID.randomUUID();
    EnrollmentCreated event =
        new EnrollmentCreated(enrollmentId, UUID.randomUUID(), UUID.randomUUID(), PRICE);
    scenario
        .publish(event)
        .andWaitForStateChange(() -> receivables.findById(enrollmentId).orElse(null))
        .andVerify(r -> assertThat(r.getAmountDueVnd()).isEqualTo(PRICE));
    // phát lại (Event Publication Registry) không tạo bản thứ hai, không lỗi
    scenario
        .publish(event)
        .andWaitForStateChange(() -> receivables.findById(enrollmentId).orElse(null))
        .andVerify(r -> assertThat(r.debtVnd()).isEqualTo(PRICE));
  }

  @Test
  void firstPaymentActivatesAndLaterPaymentKeepsExpiry(Scenario scenario) throws Exception {
    TestUser student = user(Role.STUDENT);
    TestUser otherStudent = user(Role.STUDENT);
    TestUser teacher = user(Role.TEACHER);
    UUID admin = user(Role.ADMIN).id();
    Enrollment created = newEnrollment(student.id(), teacher.id(), admin);
    UUID id = created.getId();
    // công nợ tạo cùng transaction với ghi danh (R1 verify U2)
    assertThat(receivables.findById(id)).isPresent();

    LocalDate today = LocalDate.now();
    scenario
        .stimulate(
            () -> paymentService.record(id, 1_000_000, today, Payment.Method.CASH, null, admin))
        .andWaitForEventOfType(EnrollmentActivated.class)
        .matching(e -> e.enrollmentId().equals(id))
        .toArriveAndVerify(
            e -> {
              Enrollment active = enrollments.findById(id).orElseThrow();
              assertThat(active.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
              assertThat(active.getExpiresAt())
                  .isEqualTo(active.getActivatedAt().plus(Duration.ofDays(90)))
                  // DB lưu tới micro giây
                  .isCloseTo(e.expiresAt(), within(1, ChronoUnit.MILLIS));
            });

    Instant expiresAt = enrollments.findById(id).orElseThrow().getExpiresAt();
    scenario
        .stimulate(
            () ->
                paymentService.record(
                    id, 500_000, today, Payment.Method.BANK_TRANSFER, "CK lần 2", admin))
        .andWaitForEventOfType(PaymentRecorded.class)
        .matching(e -> e.enrollmentId().equals(id) && !e.isFirstPayment())
        .toArriveAndVerify(
            e ->
                assertThat(enrollments.findById(id).orElseThrow().getExpiresAt())
                    .isEqualTo(expiresAt));

    String url = "/api/v1/enrollments/" + id + "/payments";
    call(get(url), student)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amountPaidVnd").value(1_500_000))
        .andExpect(jsonPath("$.debtVnd").value(PRICE - 1_500_000))
        .andExpect(jsonPath("$.payments.length()").value(2));
    call(get(url), otherStudent).andExpect(status().isNotFound());
    call(get(url), teacher).andExpect(status().isForbidden());
    // chỉ admin ghi thanh toán / điều chỉnh (F6 verify U2)
    String adjustment = "{\"amountVnd\":-100000,\"note\":\"Nhập nhầm\"}";
    call(post(url + "/adjustments"), teacher, adjustment).andExpect(status().isForbidden());
    call(post(url + "/adjustments"), student, adjustment).andExpect(status().isForbidden());
    call(
            post(url),
            student,
            "{\"amountVnd\":1000,\"paidAt\":\"" + today + "\",\"method\":\"CASH\"}")
        .andExpect(status().isForbidden());
  }

  private Enrollment newEnrollment(UUID studentId, UUID teacherId, UUID admin) {
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói 12", 12, 45, 90, PRICE));
    return enrollmentService.create(studentId, pkg.getId(), teacherId, null, admin);
  }
}
