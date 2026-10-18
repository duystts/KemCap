package com.englishplatform.learning.internal.path;

import com.englishplatform.learning.internal.StudentAccessGuard;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import com.englishplatform.shared.web.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lộ trình cá nhân (FR-017, FR-018): tạo, sửa cả cây kèm lý do bắt buộc, lịch sử điều chỉnh tự sinh
 * tóm tắt.
 */
@Service
public class LearningPathService {

  private final LearningPathRepository paths;
  private final PathRevisionRepository revisions;
  private final TopicRepository topics;
  private final StudentAccessGuard guard;
  private final EntityManager entityManager;

  public LearningPathService(
      LearningPathRepository paths,
      PathRevisionRepository revisions,
      TopicRepository topics,
      StudentAccessGuard guard,
      EntityManager entityManager) {
    this.paths = paths;
    this.revisions = revisions;
    this.topics = topics;
    this.guard = guard;
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public LearningPath active(CurrentUser actor, UUID studentId) {
    guard.requireRead(actor, studentId);
    return paths
        .findByStudentIdAndStatus(studentId, LearningPath.Status.ACTIVE)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Học viên chưa có lộ trình"));
  }

  /** Mỗi học viên tối đa một lộ trình đang dùng — đã có thì sửa bằng {@link #update}. */
  @Transactional
  public LearningPath create(CurrentUser actor, UUID studentId, PathSpec spec) {
    guard.requireWrite(actor, studentId);
    if (paths.findByStudentIdAndStatus(studentId, LearningPath.Status.ACTIVE).isPresent()) {
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION,
          "Học viên đã có lộ trình; hãy điều chỉnh lộ trình hiện tại");
    }
    try {
      return paths.saveAndFlush(new LearningPath(studentId, actor.id(), spec));
    } catch (DataIntegrityViolationException e) {
      // hai lượt tạo chen nhau: unique index "một lộ trình ACTIVE" chặn lượt sau
      throw new BusinessException(
          ErrorCode.INVALID_STATE_TRANSITION, "Học viên vừa được tạo lộ trình; hãy tải lại");
    }
  }

  /**
   * Sửa cả cây; {@code reason} bắt buộc; {@code version} phải khớp phiên bản đã đọc (hai người cùng
   * sửa → người sau nhận 409, NFR-REL-08). Không thay đổi gì thì không ghi lịch sử.
   */
  @Transactional
  public LearningPath update(
      CurrentUser actor, UUID pathId, PathSpec spec, String reason, Long version) {
    LearningPath path =
        paths.findById(pathId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    guard.requireWrite(actor, path.getStudentId());
    if (reason == null || reason.isBlank()) {
      throw BusinessException.validation("reason", "Cần ghi lý do điều chỉnh lộ trình");
    }
    if (version == null || version != path.getVersion()) {
      throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION);
    }
    if (path.getStatus() != LearningPath.Status.ACTIVE) {
      throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    LearningPath.Changes changes = path.apply(spec);
    if (!changes.isEmpty()) {
      topics.deleteAll(changes.removedTopics());
      // đổi chặng/chủ đề không làm "bẩn" dòng learning_path → tăng version tường minh
      entityManager.lock(path, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
      revisions.save(
          new PathRevision(
              path.getId(), actor.id(), reason.trim(), String.join("; ", changes.lines())));
      entityManager.flush();
    }
    return path;
  }

  @Transactional(readOnly = true)
  public List<PathRevision> revisions(CurrentUser actor, UUID pathId) {
    LearningPath path =
        paths.findById(pathId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    guard.requireRead(actor, path.getStudentId());
    return revisions.findByPathIdOrderByCreatedAtDesc(pathId);
  }
}
