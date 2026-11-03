package com.englishplatform.billing.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Chỉ thêm và đọc — cố ý không kế thừa {@code JpaRepository} để không có {@code delete*} (SC-007).
 */
public interface PaymentRepository extends Repository<Payment, UUID> {

  Payment save(Payment payment);

  List<Payment> findByEnrollmentIdOrderByCreatedAt(UUID enrollmentId);

  boolean existsByEnrollmentIdAndAdjustmentFalse(UUID enrollmentId);

  /** Tổng đã thu theo {@code paid_at} trong kỳ, kể cả điều chỉnh âm; null = không giới hạn. */
  @Query(
      """
      select coalesce(sum(p.amountVnd), 0) from Payment p
      where (cast(:from as date) is null or p.paidAt >= :from)
        and (cast(:to as date) is null or p.paidAt <= :to)
      """)
  long sumCollected(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
