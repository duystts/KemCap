package com.englishplatform.learning.internal.report;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionReportRepository extends JpaRepository<SessionReport, UUID> {

  Optional<SessionReport> findBySessionId(UUID sessionId);

  /** {@code teacherId}, {@code status} tùy chọn (null = bỏ qua); hạn sớm nhất trước. */
  @Query(
      """
      select r from SessionReport r
      where (:teacherId is null or r.teacherId = :teacherId)
        and (:status is null or r.status = :status)
      order by r.dueAt, r.id
      """)
  List<SessionReport> search(
      @Param("teacherId") UUID teacherId, @Param("status") SessionReport.Status status);

  /**
   * Phiếu đã nộp còn tính vào tiến bộ, buổi sớm nhất trước. Bỏ qua buổi đã sửa thành không hoàn
   * thành và phiếu đã xóa nội dung khi ẩn danh hóa.
   */
  @Query(
      """
      select r from SessionReport r
      where r.studentId = :studentId
        and r.status = com.englishplatform.learning.internal.report.SessionReport.Status.SUBMITTED
        and r.sessionVoidedAt is null and r.comment is not null
      order by r.sessionEndsAt, r.id
      """)
  List<SessionReport> findSubmittedByStudent(@Param("studentId") UUID studentId);

  @Query(
      """
      select r from SessionReport r
      where r.status = com.englishplatform.learning.internal.report.SessionReport.Status.PENDING
        and r.dueAt <= :now and r.overdueNotifiedAt is null
      """)
  List<SessionReport> findOverdue(@Param("now") Instant now);

  /** Trả 1 nếu lượt này đánh dấu đầu tiên: job chạy chồng cũng chỉ nhắc một lần. */
  @Modifying
  @Query(
      "update SessionReport r set r.overdueNotifiedAt = :now"
          + " where r.id = :id and r.overdueNotifiedAt is null")
  int markOverdueNotified(@Param("id") UUID id, @Param("now") Instant now);

  /**
   * FR-031: xóa điểm, nhận xét, chủ đề; giữ mốc thời gian và trạng thái. Đánh dấu đã nhắc để phiếu
   * còn treo của học viên đã ẩn danh không nhắc giáo viên nữa.
   */
  @Modifying
  @Query(
      """
      update SessionReport r set
        r.scoreListening = null, r.scoreSpeaking = null, r.scoreReading = null,
        r.scoreWriting = null, r.scoreGrammarVocab = null, r.comment = null,
        r.taughtTopicIds = null, r.overdueNotifiedAt = coalesce(r.overdueNotifiedAt, :now)
      where r.studentId = :studentId
      """)
  int anonymizeForStudent(@Param("studentId") UUID studentId, @Param("now") Instant now);
}
