package com.englishplatform.scheduling.internal.availability;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityExceptionRepository
    extends JpaRepository<AvailabilityException, UUID> {

  List<AvailabilityException> findByTeacherIdAndDateBetweenOrderByDate(
      UUID teacherId, LocalDate from, LocalDate to);
}
