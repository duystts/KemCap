package com.englishplatform.scheduling.internal.web;

import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.scheduling.internal.report.TeacherSessionsReportService;
import com.englishplatform.scheduling.internal.session.SessionRepository.TeacherSessionCount;
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

/** Số buổi đã dạy theo giáo viên, phục vụ tính thù lao (FR-027). */
@RestController
@RequestMapping("/api/v1/reports/teacher-sessions")
@PreAuthorize("hasRole('ADMIN')")
class TeacherSessionsReportController {

  private final TeacherSessionsReportService service;
  private final UserDirectory userDirectory;

  TeacherSessionsReportController(
      TeacherSessionsReportService service, UserDirectory userDirectory) {
    this.service = service;
    this.userDirectory = userDirectory;
  }

  record TeacherSessionsDto(UUID teacherId, String teacherName, long sessionsTaught) {}

  @GetMapping
  List<TeacherSessionsDto> report(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    List<TeacherSessionCount> counts = service.sessionsTaught(from, to);
    Map<UUID, UserView> teachers =
        userDirectory
            .findAll(counts.stream().map(TeacherSessionCount::getTeacherId).toList())
            .stream()
            .collect(Collectors.toMap(UserView::id, Function.identity()));
    return counts.stream()
        .map(
            c -> {
              UserView teacher = teachers.get(c.getTeacherId());
              return new TeacherSessionsDto(
                  c.getTeacherId(), teacher == null ? null : teacher.fullName(), c.getSessions());
            })
        .toList();
  }
}
