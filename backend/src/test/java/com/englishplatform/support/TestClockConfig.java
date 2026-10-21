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
    return MutableClock.ticking(Instant.now());
  }

  /**
   * Clock tua được. Tạo bằng constructor thì đứng yên (unit test so khớp chính xác); bean Spring
   * dùng {@link #ticking} để vẫn chạy tiếp từ mốc đã đặt như clock thật — tránh hai bản ghi tạo
   * liền nhau có cùng {@code created_at} khi entity lấy giờ từ Clock của app (R18).
   */
  public static class MutableClock extends Clock {

    private volatile Instant base;
    private volatile long baseNanos;
    private final boolean ticks;

    public MutableClock(Instant now) {
      this(now, false);
    }

    private MutableClock(Instant now, boolean ticks) {
      this.ticks = ticks;
      set(now);
    }

    public static MutableClock ticking(Instant now) {
      return new MutableClock(now, true);
    }

    public synchronized void set(Instant instant) {
      this.base = instant;
      this.baseNanos = System.nanoTime();
    }

    public synchronized void advance(Duration duration) {
      set(instant().plus(duration));
    }

    public void reset() {
      set(Instant.now());
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
      return ticks ? base.plusNanos((System.nanoTime() - baseNanos) / 1_000 * 1_000) : base;
    }
  }
}
