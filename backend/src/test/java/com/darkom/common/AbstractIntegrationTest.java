package com.darkom.common;

import java.util.UUID;
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

  /**
   * Test classes sharing this base class share one Testcontainers Postgres instance (Spring's
   * context cache reuses it whenever the context configuration matches), with no data cleanup
   * between classes - a literal email reused across two test classes can collide. Suffix with a
   * random UUID to make collisions impossible regardless of test execution order.
   */
  protected static String uniqueEmail(String localPart) {
    return localPart + "+" + UUID.randomUUID() + "@example.com";
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
      return new PostgreSQLContainer<>("postgres:16-alpine");
    }
  }
}
