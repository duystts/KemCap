package com.englishplatform.learning.internal.proficiency;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends Repository<LearningGoal, UUID> {

  LearningGoal save(LearningGoal goal);

  Optional<LearningGoal> findById(UUID id);

  List<LearningGoal> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

  @Query(
      "select distinct g.studentId from LearningGoal g where g.studentId in :ids and g.status ="
          + " com.englishplatform.learning.internal.proficiency.LearningGoal.Status.ACTIVE")
  List<UUID> findStudentsWithActiveGoal(@Param("ids") Collection<UUID> studentIds);

  @Modifying
  @Query("delete from LearningGoal g where g.studentId = :studentId")
  int deleteByStudent(@Param("studentId") UUID studentId);
}
