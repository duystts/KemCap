package com.englishplatform.billing.internal;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {

  /** Khóa công nợ khi ghi thanh toán: hai lượt ghi đồng thời không làm lệch tổng đã thu. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Receivable r where r.enrollmentId = :id")
  Optional<Receivable> findForUpdate(@Param("id") UUID enrollmentId);
}
