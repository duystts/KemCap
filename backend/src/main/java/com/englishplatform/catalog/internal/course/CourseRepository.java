package com.englishplatform.catalog.internal.course;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseRepository extends JpaRepository<Course, UUID> {

  @Query("select c from Course c where lower(c.name) = lower(?1)")
  List<Course> findByNameIgnoreCase(String name);

  List<Course> findAllByOrderByName();

  List<Course> findByStatusOrderByName(CatalogItemStatus status);
}
