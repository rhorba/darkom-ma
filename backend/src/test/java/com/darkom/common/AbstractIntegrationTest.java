package com.darkom.common;

import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

  /**
   * Without this, LeasePdfService writes real files under the project's ./data/lease-documents
   * (application.yml's default) on every test run. A src/test/resources/application.yml would
   * shadow the whole main config instead of overriding one property (Spring only loads the first
   * classpath:application.yml match, and test-classes wins) - @DynamicPropertySource layers on top
   * instead.
   */
  @DynamicPropertySource
  static void overrideLeaseDocumentsDir(DynamicPropertyRegistry registry) {
    registry.add(
        "app.storage.lease-documents-dir",
        () -> System.getProperty("java.io.tmpdir") + "/darkom-test-lease-documents");
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
