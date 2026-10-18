package com.englishplatform.billing.internal;

import com.englishplatform.billing.PaymentRecorded;
import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.EnrollmentStatus;
import com.englishplatform.catalog.EnrollmentView;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi nhận thanh toán và điều chỉnh (FR-025). Thanh toán đầu tiên (số tiền bất kỳ) kích hoạt ghi
 * danh ngay trong cùng transaction qua {@link EnrollmentApi#activateOnFirstPayment}; các lần sau
 * không đổi hạn gói.
 */
@Service
public class PaymentService {

  /** "Hôm nay" theo giờ Việt Nam khi kiểm {@code paid_at ≤ hôm nay}. */
  static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final ReceivableRepository receivables;
  private final PaymentRepository payments;
  private final EnrollmentApi enrollmentApi;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  PaymentService(
      ReceivableRepository receivables,
      PaymentRepository payments,
      EnrollmentApi enrollmentApi,
      ApplicationEventPublisher events,
      Clock clock) {
    this.receivables = receivables;
    this.payments = payments;
    this.enrollmentApi = enrollmentApi;
    this.events = events;
    this.clock = clock;
  }

  public record Statement(Receivable receivable, List<Payment> payments) {}

  @Transactional
  public Payment record(
      UUID enrollmentId,
      long amountVnd,
      LocalDate paidAt,
      Payment.Method method,
      String note,
      UUID recordedBy) {
    if (amountVnd <= 0) {
      throw BusinessException.validation("amountVnd", "Số tiền phải lớn hơn 0");
    }
    if (paidAt == null || paidAt.isAfter(today())) {
      throw BusinessException.validation("paidAt", "Ngày thanh toán không được sau hôm nay");
    }
    if (method == null) {
      throw BusinessException.validation("method", "Chọn phương thức thanh toán");
    }
    Receivable receivable = lockReceivable(enrollmentId);
    boolean first = !payments.existsByEnrollmentIdAndAdjustmentFalse(enrollmentId);
    Payment payment =
        payments.save(
            new Payment(
                enrollmentId,
                receivable.getStudentId(),
                amountVnd,
                paidAt,
                method,
                false,
                blankToNull(note),
                recordedBy));
    receivable.addPaid(amountVnd);
    if (first) {
      enrollmentApi.activateOnFirstPayment(enrollmentId);
    }
    events.publishEvent(new PaymentRecorded(enrollmentId, payment.getId(), amountVnd, first));
    return payment;
  }

  /** Điều chỉnh nhập nhầm: số tiền âm, ghi chú bắt buộc, tổng đã thu không âm. */
  @Transactional
  public Payment adjust(UUID enrollmentId, long amountVnd, String note, UUID recordedBy) {
    if (amountVnd >= 0) {
      throw BusinessException.validation("amountVnd", "Điều chỉnh phải là số âm");
    }
    if (note == null || note.isBlank()) {
      throw BusinessException.validation("note", "Cần ghi chú lý do điều chỉnh");
    }
    Receivable receivable = lockReceivable(enrollmentId);
    if (receivable.getAmountPaidVnd() + amountVnd < 0) {
      throw BusinessException.validation("amountVnd", "Điều chỉnh vượt quá tổng đã thu");
    }
    Payment payment =
        payments.save(
            new Payment(
                enrollmentId,
                receivable.getStudentId(),
                amountVnd,
                today(),
                Payment.Method.OTHER,
                true,
                note.trim(),
                recordedBy));
    receivable.addPaid(amountVnd);
    events.publishEvent(new PaymentRecorded(enrollmentId, payment.getId(), amountVnd, false));
    return payment;
  }

  /** Admin xem mọi ghi danh; học viên chỉ của mình, còn lại 404. */
  @Transactional(readOnly = true)
  public Statement statement(UUID enrollmentId, UUID viewerId, boolean admin) {
    Receivable receivable =
        receivables
            .findById(enrollmentId)
            .filter(r -> admin || r.getStudentId().equals(viewerId))
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return new Statement(receivable, payments.findByEnrollmentIdOrderByCreatedAt(enrollmentId));
  }

  private Receivable lockReceivable(UUID enrollmentId) {
    EnrollmentView enrollment = enrollmentApi.get(enrollmentId);
    if (enrollment.status() == EnrollmentStatus.CANCELLED) {
      throw new BusinessException(ErrorCode.ENROLLMENT_NOT_ACTIVE);
    }
    // công nợ được tạo cùng transaction với ghi danh (EnrollmentCreatedListener, BEFORE_COMMIT)
    return receivables
        .findForUpdate(enrollmentId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private LocalDate today() {
    return clock.instant().atZone(BUSINESS_ZONE).toLocalDate();
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
