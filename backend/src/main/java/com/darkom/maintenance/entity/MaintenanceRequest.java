package com.darkom.maintenance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest {

  @Id private UUID id;

  @Column(name = "unit_id", nullable = false)
  private UUID unitId;

  @Column(name = "reported_by", nullable = false)
  private UUID reportedBy;

  @Column(nullable = false)
  private String description;

  @Column(name = "photo_path")
  private String photoPath;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MaintenanceRequestStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUnitId() {
    return unitId;
  }

  public void setUnitId(UUID unitId) {
    this.unitId = unitId;
  }

  public UUID getReportedBy() {
    return reportedBy;
  }

  public void setReportedBy(UUID reportedBy) {
    this.reportedBy = reportedBy;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getPhotoPath() {
    return photoPath;
  }

  public void setPhotoPath(String photoPath) {
    this.photoPath = photoPath;
  }

  public MaintenanceRequestStatus getStatus() {
    return status;
  }

  public void setStatus(MaintenanceRequestStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
