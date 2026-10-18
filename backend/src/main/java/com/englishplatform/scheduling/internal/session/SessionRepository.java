package com.englishplatform.scheduling.internal.session;

import com.englishplatform.scheduling.SessionStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, UUID> {

  /** Buổi chiếm khung giờ của giáo viên hoặc học viên giao với [from, to). */
  @Query(
      """
      select s from Session s
      where (s.teacherId = :teacherId or s.studentId = :studentId)
        and s.status in :statuses and s.startsAt < :to and s.endsAt > :from
      """)
  List<Session> findOccupying(
      @Param("teacherId") UUID teacherId,
      @Param("studentId") UUID studentId,
      @Param("statuses") Collection<SessionStatus> statuses,
      @Param("from") Instant from,
      @Param("to") Instant to);

  @Query(
      """
      select s from Session s
      where s.teacherId = :teacherId and s.status = :status
        and s.startsAt < :to and s.endsAt > :from
      order by s.startsAt
      """)
  List<Session> findTeacherSessions(
      @Param("teacherId") UUID teacherId,
      @Param("status") SessionStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Lọc lịch; mọi tham số tùy chọn (null = bỏ qua). */
  @Query(
      """
      select s from Session s
      where (:teacherId is null or s.teacherId = :teacherId)
        and (:studentId is null or s.studentId = :studentId)
        and (:status is null or s.status = :status)
        and (cast(:from as timestamp) is null or s.endsAt > :from)
        and (cast(:to as timestamp) is null or s.startsAt < :to)
      order by s.startsAt
      """)
  List<Session> search(
      @Param("teacherId") UUID teacherId,
      @Param("studentId") UUID studentId,
      @Param("status") SessionStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to);

  /** Buổi {@code BOOKED} đã qua giờ kết thúc nhưng chưa chốt kết quả (FR-014). */
  @Query(
      """
      select s from Session s
      where s.status = com.englishplatform.scheduling.SessionStatus.BOOKED and s.endsAt <= :now
        and (:teacherId is null or s.teacherId = :teacherId)
      order by s.endsAt
      """)
  List<Session> findOutcomePending(@Param("now") Instant now, @Param("teacherId") UUID teacherId);

  List<Session> findByEnrollmentIdAndStatusAndStartsAtAfter(
      UUID enrollmentId, SessionStatus status, Instant after);

  List<Session> findByStudentIdAndStatusAndStartsAtAfter(
      UUID studentId, SessionStatus status, Instant after);

  List<Session> findByEnrollmentId(UUID enrollmentId);

  /** Ứng viên nhắc lịch: {@code BOOKED}, bắt đầu trong (now, until]. */
  @Query(
      """
      select s from Session s
      where s.status = com.englishplatform.scheduling.SessionStatus.BOOKED
        and s.startsAt > :now and s.startsAt <= :until
      """)
  List<Session> findStartingBetween(@Param("now") Instant now, @Param("until") Instant until);

  @Query(
      """
      select s from Session s
      where s.status = com.englishplatform.scheduling.SessionStatus.BOOKED
        and s.endsAt <= :endedBefore and s.outcomeOverdueNotifiedAt is null
      """)
  List<Session> findOutcomeOverdue(@Param("endedBefore") Instant endedBefore);

  // Cờ phụ cập nhật bằng update có điều kiện, KHÔNG tăng @Version: không xung đột với thao tác của
  // người dùng trên cùng buổi (vd. giáo viên + học viên vào phòng cùng lúc), và chỉ một lượt thắng
  // nếu job chạy chồng (trả 1 = lượt này đánh dấu đầu tiên).

  @Modifying
  @Query(
      "update Session s set s.teacherJoinedAt = :now where s.id = :id and s.teacherJoinedAt is null")
  int markTeacherJoined(@Param("id") UUID id, @Param("now") Instant now);

  @Modifying
  @Query(
      "update Session s set s.studentJoinedAt = :now where s.id = :id and s.studentJoinedAt is null")
  int markStudentJoined(@Param("id") UUID id, @Param("now") Instant now);

  @Modifying
  @Query("update Session s set s.reminder24hAt = :now where s.id = :id and s.reminder24hAt is null")
  int markReminder24h(@Param("id") UUID id, @Param("now") Instant now);

  @Modifying
  @Query("update Session s set s.reminder1hAt = :now where s.id = :id and s.reminder1hAt is null")
  int markReminder1h(@Param("id") UUID id, @Param("now") Instant now);

  @Modifying
  @Query(
      "update Session s set s.outcomeOverdueNotifiedAt = :now"
          + " where s.id = :id and s.outcomeOverdueNotifiedAt is null")
  int markOutcomeOverdueNotified(@Param("id") UUID id, @Param("now") Instant now);
}
