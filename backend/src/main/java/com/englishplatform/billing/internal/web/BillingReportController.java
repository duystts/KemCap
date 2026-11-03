package com.englishplatform.billing.internal.web;

import com.englishplatform.billing.internal.BillingReportService;
import com.englishplatform.billing.internal.BillingReportService.ReceivableRow;
import com.englishplatform.billing.internal.BillingReportService.Revenue;
import com.englishplatform.billing.internal.Receivable;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Danh sách công nợ và báo cáo doanh thu cho giáo vụ (FR-025, FR-027). */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMIN')")
class BillingReportController {

  private final BillingReportService service;
  private final UserDirectory userDirectory;

  BillingReportController(BillingReportService service, UserDirectory userDirectory) {
    this.service = service;
    this.userDirectory = userDirectory;
  }

  record ReceivableDto(
      UUID enrollmentId,
      UUID studentId,
      String studentName,
      long amountDueVnd,
      long amountPaidVnd,
      long debtVnd,
      boolean cancelled) {}

  @GetMapping("/receivables")
  List<ReceivableDto> receivables(@RequestParam(required = false) Boolean hasDebt) {
    List<ReceivableRow> rows = service.receivables(hasDebt);
    Map<UUID, UserView> students =
        userDirectory
            .findAll(rows.stream().map(r -> r.receivable().getStudentId()).distinct().toList())
            .stream()
            .collect(Collectors.toMap(UserView::id, Function.identity()));
    return rows.stream()
        .map(
            row -> {
              Receivable r = row.receivable();
              UserView student = students.get(r.getStudentId());
              return new ReceivableDto(
                  r.getEnrollmentId(),
                  r.getStudentId(),
                  student == null ? null : student.fullName(),
                  r.getAmountDueVnd(),
                  r.getAmountPaidVnd(),
                  r.debtVnd(),
                  row.cancelled());
            })
        .toList();
  }

  @GetMapping("/reports/revenue")
  Revenue revenue(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return service.revenue(from, to);
  }
}
