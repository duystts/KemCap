package com.englishplatform.catalog;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.englishplatform.catalog.internal.jobs.EnrollmentLifecycleJob;
import com.englishplatform.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** R11: job gia hạn chạy bù khi app khởi động. */
@TestPropertySource(properties = "app.jobs.enrollment-lifecycle-on-startup=true")
class EnrollmentLifecycleStartupIT extends ApiTestSupport {

  @MockitoSpyBean EnrollmentLifecycleJob job;

  @Test
  void jobRanOnStartup() {
    verify(job, atLeastOnce()).run();
  }
}
