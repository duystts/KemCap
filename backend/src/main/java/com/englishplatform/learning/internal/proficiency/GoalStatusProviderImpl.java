package com.englishplatform.learning.internal.proficiency;

import com.englishplatform.catalog.GoalStatusProvider;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cài SPI của catalog cho cờ "Cần tư vấn" (OQ-4). */
@Component
class GoalStatusProviderImpl implements GoalStatusProvider {

  private final GoalRepository goals;

  GoalStatusProviderImpl(GoalRepository goals) {
    this.goals = goals;
  }

  @Override
  @Transactional(readOnly = true)
  public Set<UUID> studentsWithActiveGoal(Collection<UUID> studentIds) {
    if (studentIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(goals.findStudentsWithActiveGoal(studentIds));
  }
}
