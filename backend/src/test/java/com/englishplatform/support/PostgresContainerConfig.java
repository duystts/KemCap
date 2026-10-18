package com.englishplatform.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL thật qua Testcontainers (constitution IV — không dùng H2), tái sử dụng container. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return new PostgreSQLContainer("postgres:16").withReuse(true);
  }
}
