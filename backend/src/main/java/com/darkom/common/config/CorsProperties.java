package com.darkom.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

  private String allowedOrigin;

  public String getAllowedOrigin() {
    return allowedOrigin;
  }

  public void setAllowedOrigin(String allowedOrigin) {
    this.allowedOrigin = allowedOrigin;
  }
}
