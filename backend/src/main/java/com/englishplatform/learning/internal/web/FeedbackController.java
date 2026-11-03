package com.englishplatform.learning.internal.web;

import com.englishplatform.learning.internal.feedback.FeedbackService;
import com.englishplatform.learning.internal.feedback.SessionFeedback;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Đánh giá sao từng buổi (FR-022): học viên gửi, chỉ giáo vụ xem trong MVP. */
@RestController
@RequestMapping("/api/v1")
class FeedbackController {

  private final FeedbackService service;

  FeedbackController(FeedbackService service) {
    this.service = service;
  }

  record FeedbackDto(
      UUID sessionId,
      UUID studentId,
      UUID teacherId,
      int stars,
      String comment,
      Instant createdAt) {

    static FeedbackDto of(SessionFeedback f) {
      return new FeedbackDto(
          f.getSessionId(),
          f.getStudentId(),
          f.getTeacherId(),
          f.getStars(),
          f.getComment(),
          f.getCreatedAt());
    }
  }

  record RateRequest(@NotNull Integer stars, @Size(max = 2000) String comment) {}

  @PostMapping("/sessions/{sessionId}/feedback")
  @PreAuthorize("hasRole('STUDENT')")
  @ResponseStatus(HttpStatus.CREATED)
  FeedbackDto rate(@PathVariable UUID sessionId, @Valid @RequestBody RateRequest r) {
    return FeedbackDto.of(service.rate(CurrentUser.require(), sessionId, r.stars(), r.comment()));
  }

  /** Học viên biết buổi nào đã chấm để ẩn lời mời đánh giá. */
  @GetMapping("/session-feedback/mine")
  @PreAuthorize("hasRole('STUDENT')")
  List<FeedbackDto> mine() {
    return service.mine(CurrentUser.require()).stream().map(FeedbackDto::of).toList();
  }

  @GetMapping("/sessions/{sessionId}/feedback")
  @PreAuthorize("hasRole('ADMIN')")
  FeedbackDto get(@PathVariable UUID sessionId) {
    return FeedbackDto.of(service.get(sessionId));
  }
}
