package com.englishplatform.shared.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mọi service lấy thời gian hiện tại qua {@link Clock} (UTC) để test điều khiển được. */
@Configuration
class ClockConfig {

  @Bean
  @ConditionalOnMissingBean
  Clock clock() {
    return Clock.systemUTC();
  }
}
