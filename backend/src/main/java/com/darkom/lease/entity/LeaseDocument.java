package com.darkom.lease.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lease_documents")
public class LeaseDocument {

  @Id private UUID id;

  @Column(name = "lease_id", nullable = false, unique = true)
  private UUID leaseId;

  @Column(name = "file_path", nullable = false)
  private String filePath;

  @Column(name = "template_version", nullable = false)
  private String templateVersion;

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getLeaseId() {
    return leaseId;
  }

  public void setLeaseId(UUID leaseId) {
    this.leaseId = leaseId;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(String templateVersion) {
    this.templateVersion = templateVersion;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(Instant generatedAt) {
    this.generatedAt = generatedAt;
  }
}
