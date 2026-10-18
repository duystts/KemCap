package com.englishplatform.catalog.internal.enrollment;

import com.englishplatform.catalog.CatalogEvents.EnrollmentActivated;
import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bộ đếm buổi chạy trong transaction của bên gọi ({@code MANDATORY}): khóa dòng ghi danh, đổi bộ
 * đếm qua entity, {@code @Version} + CHECK ở DB là hàng rào cuối (NFR-REL-02, SC-007).
 */
@Service
class EnrollmentApiImpl implements EnrollmentApi {

  private final EnrollmentRepository enrollments;
  private final EntityManager entityManager;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  EnrollmentApiImpl(
      EnrollmentRepository enrollments,
      EntityManager entityManager,
      ApplicationEventPublisher events,
      Clock clock) {
    this.enrollments = enrollments;
    this.entityManager = entityManager;
    this.events = events;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public EnrollmentView get(UUID enrollmentId) {
    return enrollments
        .findById(enrollmentId)
        .map(Enrollment::toView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnrollmentView> findByStudent(UUID studentId) {
    return enrollments.search(studentId, null, null).stream().map(Enrollment::toView).toList();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void reserve(UUID enrollmentId) {
    update(enrollmentId, e -> e.reserve(clock.instant()));
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void release(UUID enrollmentId) {
    update(enrollmentId, Enrollment::release);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void consume(UUID enrollmentId) {
    update(enrollmentId, Enrollment::consume);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void consumeReleased(UUID enrollmentId) {
    update(enrollmentId, Enrollment::consumeReleased);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void unconsume(UUID enrollmentId) {
    update(enrollmentId, e -> e.unconsume(clock.instant()));
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void activateOnFirstPayment(UUID enrollmentId) {
    update(
        enrollmentId,
        e -> {
          if (e.getStatus() == EnrollmentStatus.PENDING_PAYMENT) {
            e.activate(clock.instant());
            events.publishEvent(
                new EnrollmentActivated(e.getId(), e.getStudentId(), e.getExpiresAt()));
          }
        });
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isAssigned(UUID teacherId, UUID studentId) {
    return enrollments.existsByTeacherIdAndStudentIdAndStatusIn(
        teacherId, studentId, Enrollment.ASSIGNED);
  }

  /**
   * Khóa dòng và đọc lại trạng thái mới nhất ({@code refresh} + {@code PESSIMISTIC_WRITE}): kể cả
   * khi bên gọi đã {@link #get} ghi danh trước đó trong cùng transaction, bản trong bộ nhớ không bị
   * cũ — lượt đặt thua trả {@code NO_SESSIONS_LEFT} chứ không phải xung đột phiên bản.
   */
  private void update(UUID enrollmentId, Consumer<Enrollment> change) {
    Enrollment enrollment = entityManager.find(Enrollment.class, enrollmentId);
    if (enrollment == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    // ghi thay đổi chưa flush trước (vd. reserve rồi consume trong cùng transaction) — refresh sẽ
    // ghi đè trạng thái trong bộ nhớ bằng dữ liệu DB
    entityManager.flush();
    entityManager.refresh(enrollment, LockModeType.PESSIMISTIC_WRITE);
    change.accept(enrollment);
  }
}
