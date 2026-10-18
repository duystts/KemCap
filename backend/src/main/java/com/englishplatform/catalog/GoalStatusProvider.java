package com.englishplatform.catalog;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * SPI: catalog hỏi học viên nào đang có mục tiêu học tập {@code ACTIVE} để tính cờ "Cần tư vấn"
 * (quyết định 2026-10-18, OQ-4). Module learning cài; đảo chiều phụ thuộc vì learning đã dùng
 * {@link EnrollmentApi} — catalog gọi trực tiếp learning sẽ tạo vòng.
 */
public interface GoalStatusProvider {

  Set<UUID> studentsWithActiveGoal(Collection<UUID> studentIds);
}
