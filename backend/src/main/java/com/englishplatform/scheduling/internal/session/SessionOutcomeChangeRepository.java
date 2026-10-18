package com.englishplatform.scheduling.internal.session;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Chỉ thêm và đọc — không có {@code delete*}. */
public interface SessionOutcomeChangeRepository extends Repository<SessionOutcomeChange, UUID> {

  SessionOutcomeChange save(SessionOutcomeChange change);

  List<SessionOutcomeChange> findBySessionIdOrderByCreatedAt(UUID sessionId);
}
