package com.englishplatform.identity.internal.privacy;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequest, UUID> {

  /** Khóa dòng: hai thao tác xử lý cùng một yêu cầu không cùng thấy {@code RECEIVED}. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from DataSubjectRequest r where r.id = ?1")
  Optional<DataSubjectRequest> findForUpdate(UUID id);

  /** {@code studentId}, {@code status} đều tùy chọn (null = bỏ qua). */
  @Query(
      """
      select r from DataSubjectRequest r
      where (:studentId is null or r.studentId = :studentId)
        and (:status is null or r.status = :status)
      order by r.receivedOn desc, r.createdAt desc
      """)
  List<DataSubjectRequest> search(
      @Param("studentId") UUID studentId, @Param("status") DataSubjectRequest.Status status);
}
