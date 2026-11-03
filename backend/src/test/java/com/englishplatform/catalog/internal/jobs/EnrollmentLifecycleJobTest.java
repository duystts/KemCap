package com.englishplatform.catalog.internal.jobs;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishplatform.catalog.internal.enrollment.EnrollmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Một ghi danh lỗi không chặn các ghi danh khác; lỗi lúc khởi động không làm app chết. */
class EnrollmentLifecycleJobTest {

  private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");

  @Test
  void failureOnOneEnrollmentDoesNotStopTheOthers() {
    EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    EnrollmentLifecycleStep step = mock(EnrollmentLifecycleStep.class);
    UUID broken = UUID.randomUUID();
    UUID next = UUID.randomUUID();
    when(enrollments.findIdsByStatusIn(any())).thenReturn(List.of(broken, next));
    doThrow(new IllegalStateException("lỗi bất kỳ")).when(step).process(eq(broken), any());

    new EnrollmentLifecycleJob(enrollments, step, Clock.fixed(NOW, ZoneOffset.UTC)).run();

    verify(step).process(next, NOW);
  }

  @Test
  void startupCatchUpRunsJobAndSurvivesFailure() {
    EnrollmentLifecycleJob job = mock(EnrollmentLifecycleJob.class);
    EnrollmentLifecycleStartup startup = new EnrollmentLifecycleStartup(job);

    startup.catchUp();
    verify(job).run();

    doThrow(new IllegalStateException("DB bận")).when(job).run();
    assertThatCode(startup::catchUp).doesNotThrowAnyException();
  }
}
