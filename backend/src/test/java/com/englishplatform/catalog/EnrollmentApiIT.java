package com.englishplatform.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.Enrollment;
import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.Role;
import com.englishplatform.identity.internal.UserAccount;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import com.englishplatform.support.ApiTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** {@link EnrollmentApi} trên PostgreSQL thật: bộ đếm không bao giờ lệch (NFR-REL-02, SC-007). */
class EnrollmentApiIT extends ApiTestSupport {

  @Autowired EnrollmentApi enrollmentApi;
  @Autowired EnrollmentService enrollmentService;
  @Autowired EnrollmentRepository enrollments;
  @Autowired CourseService courseService;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager txManager;

  @Test
  void twoConcurrentReservationsOfLastSessionOnlyOneWins() throws Exception {
    UUID id = activeEnrollment(1);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<ErrorCode>> results = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  try {
                    inTx(() -> enrollmentApi.reserve(id));
                    return null;
                  } catch (BusinessException e) {
                    return e.code();
                  }
                }));
      }
      start.countDown();
      List<ErrorCode> outcomes = new ArrayList<>();
      for (Future<ErrorCode> f : results) {
        outcomes.add(f.get(30, TimeUnit.SECONDS));
      }
      assertThat(outcomes).containsExactlyInAnyOrder(null, ErrorCode.NO_SESSIONS_LEFT);
    } finally {
      pool.shutdownNow();
    }
    EnrollmentView view = enrollmentApi.get(id);
    assertThat(view.sessionsReserved()).isEqualTo(1);
    assertThat(view.sessionsRemaining()).isZero();
  }

  @Test
  void staleReadInCallerTransactionStillGetsNoSessionsLeft() {
    UUID id = activeEnrollment(1);
    TransactionTemplate outer = new TransactionTemplate(txManager);
    TransactionTemplate inner = new TransactionTemplate(txManager);
    inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThatThrownBy(
            () ->
                outer.executeWithoutResult(
                    status -> {
                      assertThat(enrollmentApi.get(id).sessionsRemaining()).isEqualTo(1);
                      // lượt đặt khác lấy buổi cuối và commit trước
                      inner.executeWithoutResult(s -> enrollmentApi.reserve(id));
                      enrollmentApi.reserve(id);
                    }))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).code())
        .isEqualTo(ErrorCode.NO_SESSIONS_LEFT);
  }

  @Test
  void eachCounterOperationKeepsTotals() {
    UUID id = activeEnrollment(3);
    inTx(() -> enrollmentApi.reserve(id));
    inTx(() -> enrollmentApi.reserve(id));
    assertCounters(id, 2, 0, 1);

    inTx(() -> enrollmentApi.consume(id)); // hoàn thành
    assertCounters(id, 1, 1, 1);

    inTx(() -> enrollmentApi.release(id)); // hủy đúng hạn
    assertCounters(id, 0, 1, 2);

    inTx(() -> enrollmentApi.consumeReleased(id)); // gián đoạn → tính là đã học
    assertCounters(id, 0, 2, 1);

    inTx(() -> enrollmentApi.reserve(id));
    inTx(() -> enrollmentApi.consume(id));
    assertCounters(id, 0, 3, 0);
    assertThat(enrollmentApi.get(id).status()).isEqualTo(EnrollmentStatus.EXHAUSTED);
    assertThatThrownBy(() -> inTx(() -> enrollmentApi.reserve(id)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).code())
        .isEqualTo(ErrorCode.ENROLLMENT_NOT_ACTIVE);
  }

  @Test
  void pendingPaymentCannotReserve() {
    UUID id = enrollment(2);
    assertThatThrownBy(() -> inTx(() -> enrollmentApi.reserve(id)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).code())
        .isEqualTo(ErrorCode.ENROLLMENT_NOT_ACTIVE);
  }

  @Test
  void counterChangesRequireCallerTransaction() {
    UUID id = activeEnrollment(2);
    assertThatThrownBy(() -> enrollmentApi.reserve(id))
        .isInstanceOf(IllegalTransactionStateException.class);
  }

  @Test
  void databaseCheckRejectsOverbooking() {
    UUID id = activeEnrollment(2);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update enrollment set sessions_reserved = 2, sessions_consumed = 1 where id = ?",
                    id))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void concurrentAdminEditsFailWithOptimisticLock() {
    UUID id = activeEnrollment(2);
    UserAccount otherTeacher = activeAccount(Role.TEACHER);
    UserAccount thirdTeacher = activeAccount(Role.TEACHER);
    UUID admin = activeAccount(Role.ADMIN).getId();
    TransactionTemplate outer = new TransactionTemplate(txManager);
    TransactionTemplate inner = new TransactionTemplate(txManager);
    inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    assertThatThrownBy(
            () ->
                outer.executeWithoutResult(
                    status -> {
                      Enrollment stale = enrollments.findById(id).orElseThrow();
                      // người khác sửa và commit trước
                      inner.executeWithoutResult(
                          s ->
                              enrollmentService.changeTeacher(
                                  id, otherTeacher.getId(), "Đổi lịch dạy", admin));
                      stale.changeTeacher(thirdTeacher.getId());
                    }))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
  }

  // ---- helpers ----

  private UUID enrollment(int sessions) {
    UserAccount student = activeAccount(Role.STUDENT);
    UserAccount teacher = activeAccount(Role.TEACHER);
    UUID admin = activeAccount(Role.ADMIN).getId();
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói thử", sessions, 45, 90, 1_000_000));
    return enrollmentService
        .create(student.getId(), pkg.getId(), teacher.getId(), null, admin)
        .getId();
  }

  private UUID activeEnrollment(int sessions) {
    UUID id = enrollment(sessions);
    inTx(() -> enrollmentApi.activateOnFirstPayment(id));
    return id;
  }

  private void inTx(Runnable action) {
    new TransactionTemplate(txManager).executeWithoutResult(status -> action.run());
  }

  private void assertCounters(UUID id, int reserved, int consumed, int remaining) {
    EnrollmentView view = enrollmentApi.get(id);
    assertThat(view.sessionsReserved()).isEqualTo(reserved);
    assertThat(view.sessionsConsumed()).isEqualTo(consumed);
    assertThat(view.sessionsRemaining()).isEqualTo(remaining);
  }
}
