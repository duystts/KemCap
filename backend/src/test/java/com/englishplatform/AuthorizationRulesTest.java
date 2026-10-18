package com.englishplatform;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mọi endpoint phải khai báo quyền bằng {@code @PreAuthorize} (ở method hoặc class), trừ các
 * endpoint công khai trong {@link #PUBLIC_CONTROLLERS} (NFR-SEC-01). Endpoint mới quên annotation
 * sẽ làm test này đỏ thay vì âm thầm mở cho mọi người đã đăng nhập.
 */
class AuthorizationRulesTest {

  static final Set<String> PUBLIC_CONTROLLERS =
      Set.of("com.englishplatform.identity.internal.web.AuthController");

  @Test
  void everyEndpointDeclaresAuthorization() throws Exception {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

    List<String> missing = new ArrayList<>();
    int endpoints = 0;
    for (var candidate : scanner.findCandidateComponents("com.englishplatform")) {
      Class<?> type = ClassUtils.forName(candidate.getBeanClassName(), null);
      if (PUBLIC_CONTROLLERS.contains(type.getName())) {
        continue;
      }
      boolean classLevel = AnnotatedElementUtils.hasAnnotation(type, PreAuthorize.class);
      for (Method method : type.getDeclaredMethods()) {
        if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
          continue;
        }
        endpoints++;
        if (!classLevel && !AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class)) {
          missing.add(type.getSimpleName() + "#" + method.getName());
        }
      }
    }
    assertThat(endpoints).isPositive();
    assertThat(missing).as("endpoint thiếu @PreAuthorize").isEmpty();
  }
}
