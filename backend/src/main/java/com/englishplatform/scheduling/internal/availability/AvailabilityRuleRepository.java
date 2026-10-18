package com.englishplatform.scheduling.internal.availability;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, UUID> {

  List<AvailabilityRule> findByTeacherIdOrderByDayOfWeekAscStartTimeAsc(UUID teacherId);

  @Modifying(flushAutomatically = true)
  @Query("delete from AvailabilityRule r where r.teacherId = ?1")
  void deleteByTeacher(UUID teacherId);
}
