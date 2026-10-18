package com.englishplatform.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Bật {@code @Async} cho {@code @ApplicationModuleListener}: listener chạy trên executor mặc định
 * của Spring Boot ({@code spring.task.execution.*}), không chặn thread request (vd. gửi email
 * forgot-password không làm lộ email tồn tại qua thời gian phản hồi).
 */
@Configuration
@EnableAsync
class AsyncConfig {}
