package com.darkom.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Nested {@code @TestConfiguration} auto-detection only scans classes declared directly on the test
 * class itself, not inherited ones - so subclasses would silently lose the Testcontainers wiring
 * without this explicit {@code @Import}.
 */
@SpringBootTest
@Testcontainers
@Import(AbstractIntegrationTest.ContainersConfig.class)
public abstract class AbstractIntegrationTest {

  @TestConfiguration(proxyBeanMethods = false)
  static class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
      return new PostgreSQLContainer<>("postgres:16-alpine");
    }
  }
}
