package com.englishplatform.scheduling.internal.report;

import com.englishplatform.scheduling.SessionStatus;
import com.englishplatform.scheduling.internal.session.SessionRepository;
import com.englishplatform.scheduling.internal.session.SessionRepository.TeacherSessionCount;
import com.englishplatform.shared.web.BusinessException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Số buổi đã dạy theo giáo viên (FR-027): học viên bị trừ buổi = hoàn thành, học viên vắng, hủy
 * muộn; Gián đoạn được tính là đã học đã chuyển thành {@code COMPLETED}. {@code from}, {@code to}
 * là ngày theo giờ Việt Nam, tính cả hai đầu, xét theo giờ bắt đầu buổi.
 */
@Service
public class TeacherSessionsReportService {

  static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  static final Set<SessionStatus> TAUGHT =
      EnumSet.of(
          SessionStatus.COMPLETED, SessionStatus.STUDENT_NO_SHOW, SessionStatus.CANCELLED_LATE);

  private final SessionRepository sessions;

  TeacherSessionsReportService(SessionRepository sessions) {
    this.sessions = sessions;
  }

  @Transactional(readOnly = true)
  public List<TeacherSessionCount> sessionsTaught(LocalDate from, LocalDate to) {
    if (from != null && to != null && from.isAfter(to)) {
      throw BusinessException.validation("from", "Ngày bắt đầu phải trước ngày kết thúc");
    }
    Instant start = from == null ? null : from.atStartOfDay(BUSINESS_ZONE).toInstant();
    Instant end = to == null ? null : to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
    return sessions.countByTeacher(TAUGHT, start, end);
  }
}
