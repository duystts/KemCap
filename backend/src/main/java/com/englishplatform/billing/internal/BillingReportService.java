package com.englishplatform.billing.internal;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.shared.web.BusinessException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo cáo tài chính (FR-025, FR-027). Đã thu tính theo kỳ; công nợ là số dư hiện tại, không theo
 * kỳ. Ghi danh đã hủy không tính vào công nợ nhưng vẫn giữ bản ghi và số đã thu. Thu vượt ở ghi
 * danh này không bù cho công nợ của ghi danh khác.
 */
@Service
public class BillingReportService {

  private final ReceivableRepository receivables;
  private final PaymentRepository payments;
  private final EnrollmentApi enrollmentApi;

  BillingReportService(
      ReceivableRepository receivables, PaymentRepository payments, EnrollmentApi enrollmentApi) {
    this.receivables = receivables;
    this.payments = payments;
    this.enrollmentApi = enrollmentApi;
  }

  public record ReceivableRow(Receivable receivable, boolean cancelled) {

    boolean owes() {
      return !cancelled && receivable.debtVnd() > 0;
    }
  }

  public record Revenue(LocalDate from, LocalDate to, long collectedVnd, long debtVnd) {}

  /** {@code hasDebt} null = mọi bản ghi; nợ nhiều nhất trước. */
  @Transactional(readOnly = true)
  public List<ReceivableRow> receivables(Boolean hasDebt) {
    return rows().stream()
        .filter(r -> hasDebt == null || r.owes() == hasDebt)
        .sorted(Comparator.comparingLong((ReceivableRow r) -> r.receivable().debtVnd()).reversed())
        .toList();
  }

  @Transactional(readOnly = true)
  public Revenue revenue(LocalDate from, LocalDate to) {
    if (from != null && to != null && from.isAfter(to)) {
      throw BusinessException.validation("from", "Ngày bắt đầu phải trước ngày kết thúc");
    }
    long debt =
        rows().stream().filter(ReceivableRow::owes).mapToLong(r -> r.receivable().debtVnd()).sum();
    return new Revenue(from, to, payments.sumCollected(from, to), debt);
  }

  private List<ReceivableRow> rows() {
    Set<java.util.UUID> cancelled = enrollmentApi.findCancelledIds();
    return receivables.findAll().stream()
        .map(r -> new ReceivableRow(r, cancelled.contains(r.getEnrollmentId())))
        .toList();
  }
}
