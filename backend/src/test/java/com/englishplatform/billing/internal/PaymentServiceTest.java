package com.englishplatform.billing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishplatform.billing.PaymentRecorded;
import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/** Quy tắc học phí/công nợ (FR-025, NFR-REL-03) — không cần DB. */
class PaymentServiceTest {

  // 2026-11-01 08:00 giờ Việt Nam
  static final Instant NOW = Instant.parse("2026-11-01T01:00:00Z");
  static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

  ReceivableRepository receivables = mock(ReceivableRepository.class);
  PaymentRepository payments = mock(PaymentRepository.class);
  EnrollmentApi enrollmentApi = mock(EnrollmentApi.class);
  ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  PaymentService service =
      new PaymentService(
          receivables, payments, enrollmentApi, events, Clock.fixed(NOW, ZoneOffset.UTC));

  UUID enrollmentId = UUID.randomUUID();
  UUID admin = UUID.randomUUID();
  Receivable receivable = new Receivable(enrollmentId, UUID.randomUUID(), 3_000_000);

  @BeforeEach
  void setUp() {
    when(enrollmentApi.get(enrollmentId)).thenReturn(view(EnrollmentStatus.PENDING_PAYMENT));
    when(receivables.findForUpdate(enrollmentId)).thenReturn(Optional.of(receivable));
    when(payments.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void amountMustBePositive() {
    assertField(
        () -> service.record(enrollmentId, 0, TODAY, Payment.Method.CASH, null, admin),
        "amountVnd");
  }

  @Test
  void paidAtCannotBeInTheFuture() {
    assertField(
        () ->
            service.record(
                enrollmentId, 1_000, TODAY.plusDays(1), Payment.Method.CASH, null, admin),
        "paidAt");
  }

  @Test
  void firstPaymentActivatesAndLaterPaymentsDoNot() {
    when(payments.existsByEnrollmentIdAndAdjustmentFalse(enrollmentId)).thenReturn(false);
    service.record(enrollmentId, 1_500_000, TODAY, Payment.Method.BANK_TRANSFER, "CK", admin);
    verify(enrollmentApi).activateOnFirstPayment(enrollmentId);
    assertThat(publishedEvent().isFirstPayment()).isTrue();
  }

  @Test
  void laterPaymentDoesNotTouchActivation() {
    when(enrollmentApi.get(enrollmentId)).thenReturn(view(EnrollmentStatus.ACTIVE));
    when(payments.existsByEnrollmentIdAndAdjustmentFalse(enrollmentId)).thenReturn(true);
    service.record(enrollmentId, 500_000, TODAY, Payment.Method.CASH, null, admin);
    verify(enrollmentApi, never()).activateOnFirstPayment(any());
    assertThat(publishedEvent().isFirstPayment()).isFalse();
  }

  @Test
  void debtIsDueMinusPaidIncludingAdjustments() {
    service.record(enrollmentId, 2_000_000, TODAY, Payment.Method.CASH, null, admin);
    assertThat(receivable.debtVnd()).isEqualTo(1_000_000);
    service.adjust(enrollmentId, -500_000, "Nhập nhầm số tiền", admin);
    assertThat(receivable.getAmountPaidVnd()).isEqualTo(1_500_000);
    assertThat(receivable.debtVnd()).isEqualTo(1_500_000);
  }

  @Test
  void adjustmentMustBeNegativeWithNoteAndNotExceedPaid() {
    assertField(() -> service.adjust(enrollmentId, 100, "x", admin), "amountVnd");
    assertField(() -> service.adjust(enrollmentId, -100, " ", admin), "note");
    assertField(() -> service.adjust(enrollmentId, -100, "Nhập nhầm", admin), "amountVnd");
  }

  @Test
  void cancelledEnrollmentCannotBePaid() {
    when(enrollmentApi.get(enrollmentId)).thenReturn(view(EnrollmentStatus.CANCELLED));
    assertThatThrownBy(
            () -> service.record(enrollmentId, 1_000, TODAY, Payment.Method.CASH, null, admin))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).code())
        .isEqualTo(ErrorCode.ENROLLMENT_NOT_ACTIVE);
  }

  private PaymentRecorded publishedEvent() {
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(events).publishEvent(captor.capture());
    return (PaymentRecorded) captor.getValue();
  }

  private EnrollmentView view(EnrollmentStatus status) {
    return new EnrollmentView(
        enrollmentId, UUID.randomUUID(), UUID.randomUUID(), 12, 0, 0, 45, null, status);
  }

  private static void assertField(Runnable action, String field) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e -> {
              BusinessException be = (BusinessException) e;
              assertThat(be.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
              assertThat(be.errors())
                  .extracting(BusinessException.FieldError::field)
                  .contains(field);
            });
  }
}
