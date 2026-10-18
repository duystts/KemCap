package com.englishplatform.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** NFR-SEC-05: app không khởi động khi JWT secret < 256 bit. */
class SecurityPropertiesTest {

  @Configuration
  @EnableConfigurationProperties(SecurityProperties.class)
  static class Config {}

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(Config.class);

  @Test
  void contextFailsWithShortSecret() {
    runner
        .withPropertyValues("app.security.jwt-secret=0123456789abcdef")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void contextStartsWith32ByteSecret() {
    runner
        .withPropertyValues("app.security.jwt-secret=0123456789abcdef0123456789abcdef")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void rejectsMissingSecret() {
    assertThatThrownBy(() -> new SecurityProperties(null, 15, 7))
        .isInstanceOf(IllegalStateException.class);
  }
}
