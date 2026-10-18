package com.englishplatform.learning.internal.web;

import com.englishplatform.learning.internal.path.LearningPath;
import com.englishplatform.learning.internal.path.LearningPathService;
import com.englishplatform.learning.internal.path.Milestone;
import com.englishplatform.learning.internal.path.PathRevision;
import com.englishplatform.learning.internal.path.PathSpec;
import com.englishplatform.learning.internal.path.Topic;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lộ trình cá nhân và lịch sử điều chỉnh (FR-017, FR-018). */
@RestController
@RequestMapping("/api/v1")
class LearningPathController {

  private final LearningPathService service;

  LearningPathController(LearningPathService service) {
    this.service = service;
  }

  record TopicDto(UUID id, int position, String title, String objective, Topic.Status status) {}

  record MilestoneDto(
      UUID id,
      int position,
      String title,
      LocalDate targetDate,
      Milestone.Status status,
      List<TopicDto> topics) {}

  record PathDto(
      UUID id,
      UUID studentId,
      String title,
      LearningPath.Status status,
      int completionPercent,
      List<MilestoneDto> milestones,
      long version) {

    static PathDto of(LearningPath p) {
      return new PathDto(
          p.getId(),
          p.getStudentId(),
          p.getTitle(),
          p.getStatus(),
          p.completionPercent(),
          p.getMilestones().stream()
              .map(
                  m ->
                      new MilestoneDto(
                          m.getId(),
                          m.getPosition(),
                          m.getTitle(),
                          m.getTargetDate(),
                          m.getStatus(),
                          m.getTopics().stream()
                              .map(
                                  t ->
                                      new TopicDto(
                                          t.getId(),
                                          t.getPosition(),
                                          t.getTitle(),
                                          t.getObjective(),
                                          t.getStatus()))
                              .toList()))
              .toList(),
          p.getVersion());
    }
  }

  record CreatePathRequest(@NotNull String title, List<PathSpec.MilestoneSpec> milestones) {}

  /** {@code version} = giá trị {@code version} của lần đọc gần nhất (chống ghi đè). */
  record UpdatePathRequest(
      @NotNull String title,
      List<PathSpec.MilestoneSpec> milestones,
      String reason,
      @NotNull Long version) {}

  record RevisionDto(UUID id, UUID changedBy, String reason, String summary, Instant changedAt) {
    static RevisionDto of(PathRevision r) {
      return new RevisionDto(
          r.getId(), r.getChangedBy(), r.getReason(), r.getSummary(), r.getCreatedAt());
    }
  }

  @GetMapping("/students/{studentId}/learning-path")
  @PreAuthorize("isAuthenticated()")
  // map DTO trong transaction: chặng/chủ đề nạp lười (open-in-view tắt)
  @Transactional(readOnly = true)
  PathDto active(@PathVariable UUID studentId) {
    return PathDto.of(service.active(CurrentUser.require(), studentId));
  }

  @PostMapping("/students/{studentId}/learning-path")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  @ResponseStatus(HttpStatus.CREATED)
  PathDto create(@PathVariable UUID studentId, @Valid @RequestBody CreatePathRequest r) {
    return PathDto.of(
        service.create(CurrentUser.require(), studentId, new PathSpec(r.title(), r.milestones())));
  }

  /** Thay cả cây; {@code reason} bắt buộc → 400 nếu thiếu. */
  @PutMapping("/learning-paths/{pathId}")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  @Transactional
  PathDto update(@PathVariable UUID pathId, @Valid @RequestBody UpdatePathRequest r) {
    return PathDto.of(
        service.update(
            CurrentUser.require(),
            pathId,
            new PathSpec(r.title(), r.milestones()),
            r.reason(),
            r.version()));
  }

  @GetMapping("/learning-paths/{pathId}/revisions")
  @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
  List<RevisionDto> revisions(@PathVariable UUID pathId) {
    return service.revisions(CurrentUser.require(), pathId).stream().map(RevisionDto::of).toList();
  }
}
