package com.englishplatform.learning.internal.path;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
      @Size(max = 200) String title,
      LocalDate targetDate,
      Milestone.Status status,
      @Size(max = 100) List<@Valid TopicSpec> topics) {}

  public record TopicSpec(
      UUID id, @Size(max = 200) String title, @Size(max = 2000) String objective) {}
}
