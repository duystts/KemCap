package com.englishplatform.notification.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, UUID> {

  boolean existsByDedupeKey(String dedupeKey);

  Optional<NotificationDispatch> findByDedupeKey(String dedupeKey);

  List<NotificationDispatch> findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
      NotificationDispatch.Status status, Instant now, Limit limit);
}
