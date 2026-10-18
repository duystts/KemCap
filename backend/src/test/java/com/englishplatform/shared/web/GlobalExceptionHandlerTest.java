package com.englishplatform.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Xung đột {@code @Version} (hai người sửa cùng lúc) → 409 {@code CONCURRENT_MODIFICATION}. */
class GlobalExceptionHandlerTest {

  @Test
  void optimisticLockMapsTo409() {
    ProblemDetail pd =
        new GlobalExceptionHandler()
            .handleOptimisticLock(
                new ObjectOptimisticLockingFailureException(Object.class, "enrollment-id"));
    assertThat(pd.getStatus()).isEqualTo(409);
    assertThat(pd.getProperties()).containsEntry("code", "CONCURRENT_MODIFICATION");
  }
}
