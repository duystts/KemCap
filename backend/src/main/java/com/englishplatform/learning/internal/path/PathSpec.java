package com.englishplatform.learning.internal.path;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Cây lộ trình gửi lên khi tạo/sửa: chặng và chủ đề theo thứ tự hiển thị. {@code id} null = mới;
 * phần tử cũ không còn trong cây = xóa.
 */
public record PathSpec(String title, List<MilestoneSpec> milestones) {

  public record MilestoneSpec(
      UUID id,
      String title,
      LocalDate targetDate,
      Milestone.Status status,
      List<TopicSpec> topics) {}

  public record TopicSpec(UUID id, String title, String objective) {}
}
