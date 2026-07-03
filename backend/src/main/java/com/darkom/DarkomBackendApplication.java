package com.darkom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DarkomBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(DarkomBackendApplication.class, args);
  }
}
