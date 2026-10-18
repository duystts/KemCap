package com.englishplatform.learning.internal.path;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Lịch sử điều chỉnh chỉ thêm và đọc. */
public interface PathRevisionRepository extends Repository<PathRevision, UUID> {

  PathRevision save(PathRevision revision);

  List<PathRevision> findByPathIdOrderByCreatedAtDesc(UUID pathId);
}
