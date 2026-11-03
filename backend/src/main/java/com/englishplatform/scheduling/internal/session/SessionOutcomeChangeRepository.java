package com.englishplatform.scheduling.internal.session;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Chỉ thêm và đọc — không có {@code delete*}; ngoại lệ duy nhất là xóa ghi chú khi ẩn danh hóa. */
public interface SessionOutcomeChangeRepository extends Repository<SessionOutcomeChange, UUID> {

  SessionOutcomeChange save(SessionOutcomeChange change);

  List<SessionOutcomeChange> findBySessionIdOrderByCreatedAt(UUID sessionId);

  /** FR-031: ghi chú là văn bản tự do, có thể nhắc học viên; giữ ai đổi, đổi gì và khi nào. */
  @Modifying
  @Query(
      """
      update SessionOutcomeChange c set c.note = null
      where c.note is not null
        and c.sessionId in (select s.id from Session s where s.studentId = :studentId)
      """)
  int clearNotesOf(@Param("studentId") UUID studentId);
}
