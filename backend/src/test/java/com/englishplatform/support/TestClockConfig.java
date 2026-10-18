package com.englishplatform.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Clock điều khiển được trong test: {@code clock.advance(Duration.ofMinutes(31))}. */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

  @Bean
  @Primary
  MutableClock testClock() {
    return new MutableClock(Instant.now());
  }

  public static class MutableClock extends Clock {

    private volatile Instant now;

    public MutableClock(Instant now) {
      this.now = now;
    }

    public void set(Instant instant) {
      this.now = instant;
    }

    public void advance(Duration duration) {
      this.now = now.plus(duration);
    }

    public void reset() {
      this.now = Instant.now();
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
