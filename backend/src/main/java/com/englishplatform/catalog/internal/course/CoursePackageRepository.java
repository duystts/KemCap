package com.englishplatform.catalog.internal.course;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoursePackageRepository extends JpaRepository<CoursePackage, UUID> {

  /** {@code courseId}, {@code status} tùy chọn (null = bỏ qua). */
  @Query(
      """
      select p from CoursePackage p
      where (:courseId is null or p.courseId = :courseId)
        and (:status is null or p.status = :status)
      order by p.name
      """)
  List<CoursePackage> search(
      @Param("courseId") UUID courseId, @Param("status") CatalogItemStatus status);
}
