package com.englishplatform.billing.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Chỉ thêm và đọc — cố ý không kế thừa {@code JpaRepository} để không có {@code delete*} (SC-007).
 */
public interface PaymentRepository extends Repository<Payment, UUID> {

  Payment save(Payment payment);

  List<Payment> findByEnrollmentIdOrderByCreatedAt(UUID enrollmentId);

  boolean existsByEnrollmentIdAndAdjustmentFalse(UUID enrollmentId);
}
