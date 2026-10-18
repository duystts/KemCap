package com.englishplatform.catalog.internal.enrollment;

import com.englishplatform.catalog.EnrollmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

  boolean existsByTeacherIdAndStudentIdAndStatusIn(
      UUID teacherId, UUID studentId, Collection<EnrollmentStatus> statuses);

  boolean existsByTeacherIdAndStatusIn(UUID teacherId, Collection<EnrollmentStatus> statuses);

  @Query(
      "select distinct e.studentId from Enrollment e"
          + " where e.teacherId = :teacherId and e.status in :statuses")
  List<UUID> findStudentIdsByTeacher(
      @Param("teacherId") UUID teacherId, @Param("statuses") Collection<EnrollmentStatus> statuses);

  /** Mọi tham số tùy chọn (null = bỏ qua); mới nhất trước. */
  @Query(
      """
      select e from Enrollment e
      where (:studentId is null or e.studentId = :studentId)
        and (:teacherId is null or e.teacherId = :teacherId)
        and (:status is null or e.status = :status)
      order by e.createdAt desc
      """)
  List<Enrollment> search(
      @Param("studentId") UUID studentId,
      @Param("teacherId") UUID teacherId,
      @Param("status") EnrollmentStatus status);

  List<Enrollment> findByStudentIdIn(Collection<UUID> studentIds);
}
