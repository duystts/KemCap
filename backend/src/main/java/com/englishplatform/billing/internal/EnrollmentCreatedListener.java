package com.englishplatform.billing.internal;

import com.englishplatform.catalog.CatalogEvents.EnrollmentCreated;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Ghi danh mới → công nợ bằng học phí, tạo <b>trong cùng transaction</b> với ghi danh ({@code
 * BEFORE_COMMIT}): giáo vụ ghi thanh toán ngay sau khi tạo ghi danh không bao giờ gặp "công nợ chưa
 * sẵn sàng", và ghi danh không tồn tại mà thiếu công nợ. Idempotent nếu event được phát lại.
 */
@Component
class EnrollmentCreatedListener {

  private final ReceivableRepository receivables;

  EnrollmentCreatedListener(ReceivableRepository receivables) {
    this.receivables = receivables;
  }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  void on(EnrollmentCreated event) {
    if (!receivables.existsById(event.enrollmentId())) {
      receivables.save(new Receivable(event.enrollmentId(), event.studentId(), event.priceVnd()));
    }
  }
}
