package com.englishplatform.identity.internal.web;

import com.englishplatform.identity.internal.privacy.AnonymizationService;
import com.englishplatform.identity.internal.privacy.DataSubjectRequest;
import com.englishplatform.identity.internal.privacy.DataSubjectRequestService;
import com.englishplatform.shared.security.CurrentUser;
import com.englishplatform.shared.web.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/data-subject-requests} — giáo vụ xử lý yêu cầu xem/xóa dữ liệu (FR-031). */
@RestController
@RequestMapping("/api/v1/data-subject-requests")
@PreAuthorize("hasRole('ADMIN')")
class DataSubjectRequestController {

  private final DataSubjectRequestService service;
  private final AnonymizationService anonymization;

  DataSubjectRequestController(
      DataSubjectRequestService service, AnonymizationService anonymization) {
    this.service = service;
    this.anonymization = anonymization;
  }

  record RequestDto(
      UUID id,
      UUID studentId,
      String requesterName,
      DataSubjectRequest.Type type,
      LocalDate receivedOn,
      DataSubjectRequest.Status status,
      Instant resolvedAt,
      String resolutionNote,
      UUID handledBy) {

    static RequestDto of(DataSubjectRequest r) {
      return new RequestDto(
          r.getId(),
          r.getStudentId(),
          r.getRequesterName(),
          r.getType(),
          r.getReceivedOn(),
          r.getStatus(),
          r.getResolvedAt(),
          r.getResolutionNote(),
          r.getHandledBy());
    }
  }

  record CreateRequest(
      @NotNull UUID studentId,
      @NotBlank @Size(max = 150) String requesterName,
      @NotNull DataSubjectRequest.Type type,
      @NotNull LocalDate receivedOn) {}

  record RejectRequest(@NotBlank String resolutionNote) {}

  record AnonymizeRequest(boolean confirm) {}

  @GetMapping
  List<RequestDto> list(
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) DataSubjectRequest.Status status) {
    return service.list(studentId, status).stream().map(RequestDto::of).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  RequestDto create(@Valid @RequestBody CreateRequest r) {
    return RequestDto.of(
        service.record(r.studentId(), r.requesterName().trim(), r.type(), r.receivedOn()));
  }

  @PostMapping("/{id}/reject")
  RequestDto reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest r) {
    return RequestDto.of(service.reject(id, r.resolutionNote().trim(), CurrentUser.require().id()));
  }

  @PostMapping("/{id}/complete-access")
  RequestDto completeAccess(@PathVariable UUID id) {
    return RequestDto.of(service.completeAccess(id, CurrentUser.require().id()));
  }

  @PostMapping("/{id}/anonymize")
  RequestDto anonymize(@PathVariable UUID id, @RequestBody AnonymizeRequest r) {
    if (!r.confirm()) {
      throw BusinessException.validation(
          "confirm", "Cần xác nhận: ẩn danh hóa không khôi phục được");
    }
    return RequestDto.of(anonymization.anonymize(id, CurrentUser.require().id()));
  }
}
