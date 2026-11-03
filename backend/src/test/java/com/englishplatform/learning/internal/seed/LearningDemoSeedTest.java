package com.englishplatform.learning.internal.seed;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.identity.UserDirectory;
import com.englishplatform.learning.internal.path.LearningPathRepository;
import com.englishplatform.learning.internal.proficiency.AssessmentRepository;
import com.englishplatform.learning.internal.proficiency.GoalRepository;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** F2: tài khoản demo đã bị ẩn danh hóa (đổi email) thì khởi động lại vẫn chạy được. */
class LearningDemoSeedTest {

  @Test
  void missingDemoAccountSkipsSeedInsteadOfStoppingApp() {
    UserDirectory users = mock(UserDirectory.class);
    when(users.findByEmail(any())).thenReturn(Optional.empty());
    AssessmentRepository assessments = mock(AssessmentRepository.class);
    LearningDemoSeed seed =
        new LearningDemoSeed(
            assessments,
            mock(GoalRepository.class),
            mock(LearningPathRepository.class),
            mock(EnrollmentApi.class),
            users,
            Clock.systemUTC());

    assertThatCode(() -> seed.run(null)).doesNotThrowAnyException();
    verifyNoInteractions(assessments);
  }
}
