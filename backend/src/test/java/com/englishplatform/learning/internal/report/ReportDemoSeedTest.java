package com.englishplatform.learning.internal.report;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.identity.UserView;
import com.englishplatform.learning.internal.path.LearningPathRepository;
import com.englishplatform.scheduling.SessionQueryApi;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportDemoSeedTest {

  private final SessionReportRepository reports = mock(SessionReportRepository.class);
  private final SessionQueryApi sessions = mock(SessionQueryApi.class);
  private final UserDirectory users = mock(UserDirectory.class);
  private final ReportDemoSeed seed =
      new ReportDemoSeed(
          reports, mock(LearningPathRepository.class), sessions, mock(EnrollmentApi.class), users);

  @Test
  void missingDemoAccountSkipsSeedInsteadOfStoppingApp() {
    when(users.findByEmail(any())).thenReturn(Optional.empty());

    assertThatCode(() -> seed.run(null)).doesNotThrowAnyException();
    verifyNoInteractions(reports, sessions);
  }

  @Test
  void runningAgainDoesNotCreateDuplicates() {
    UUID studentId = UUID.randomUUID();
    UserView student = mock(UserView.class);
    when(student.id()).thenReturn(studentId);
    when(users.findByEmail(any())).thenReturn(Optional.of(student));
    when(reports.findSubmittedByStudent(studentId)).thenReturn(List.of(mock(SessionReport.class)));

    seed.run(null);

    verify(reports).findSubmittedByStudent(studentId);
    verifyNoInteractions(sessions);
  }
}
