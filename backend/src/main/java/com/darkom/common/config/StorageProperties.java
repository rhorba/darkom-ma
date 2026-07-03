package com.darkom.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

  private String leaseDocumentsDir;

  public String getLeaseDocumentsDir() {
    return leaseDocumentsDir;
  }

  public void setLeaseDocumentsDir(String leaseDocumentsDir) {
    this.leaseDocumentsDir = leaseDocumentsDir;
  }
}
