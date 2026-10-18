package com.englishplatform.billing.internal.web;

import com.englishplatform.billing.internal.Payment;
import com.englishplatform.billing.internal.PaymentService;
import com.englishplatform.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /api/v1/enrollments/{id}/payments} — không có PUT/DELETE: thanh toán không sửa/xóa được
 * (NFR-REL-03).
 */
@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/payments")
class BillingController {

  private final PaymentService service;

  BillingController(PaymentService service) {
    this.service = service;
  }

  record PaymentDto(
      UUID id,
      long amountVnd,
      LocalDate paidAt,
      Payment.Method method,
      boolean adjustment,
      String note,
      UUID recordedBy,
      Instant recordedAt) {
    static PaymentDto of(Payment p) {
      return new PaymentDto(
          p.getId(),
          p.getAmountVnd(),
          p.getPaidAt(),
          p.getMethod(),
          p.isAdjustment(),
          p.getNote(),
          p.getRecordedBy(),
          p.getCreatedAt());
    }
  }

  record StatementDto(
      UUID enrollmentId,
      long amountDueVnd,
      long amountPaidVnd,
      long debtVnd,
      List<PaymentDto> payments) {}

  record PaymentRequest(
      long amountVnd, @NotNull LocalDate paidAt, @NotNull Payment.Method method, String note) {}

  record AdjustmentRequest(long amountVnd, @NotBlank String note) {}

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
  StatementDto statement(@PathVariable UUID enrollmentId) {
    CurrentUser user = CurrentUser.require();
    PaymentService.Statement s = service.statement(enrollmentId, user.id(), user.hasRole("ADMIN"));
    return new StatementDto(
        enrollmentId,
        s.receivable().getAmountDueVnd(),
        s.receivable().getAmountPaidVnd(),
        s.receivable().debtVnd(),
        s.payments().stream().map(PaymentDto::of).toList());
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  PaymentDto record(@PathVariable UUID enrollmentId, @Valid @RequestBody PaymentRequest r) {
    return PaymentDto.of(
        service.record(
            enrollmentId,
            r.amountVnd(),
            r.paidAt(),
            r.method(),
            r.note(),
            CurrentUser.require().id()));
  }

  @PostMapping("/adjustments")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  PaymentDto adjust(@PathVariable UUID enrollmentId, @Valid @RequestBody AdjustmentRequest r) {
    return PaymentDto.of(
        service.adjust(enrollmentId, r.amountVnd(), r.note(), CurrentUser.require().id()));
  }
}
