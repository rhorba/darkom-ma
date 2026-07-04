package com.darkom.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

  private String leaseDocumentsDir;
  private String maintenancePhotosDir;

  public String getLeaseDocumentsDir() {
    return leaseDocumentsDir;
  }

  public void setLeaseDocumentsDir(String leaseDocumentsDir) {
    this.leaseDocumentsDir = leaseDocumentsDir;
  }

  public String getMaintenancePhotosDir() {
    return maintenancePhotosDir;
  }

  public void setMaintenancePhotosDir(String maintenancePhotosDir) {
    this.maintenancePhotosDir = maintenancePhotosDir;
  }
}
