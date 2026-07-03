package com.darkom.property.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "units")
public class Unit {

  @Id private UUID id;

  @Column(name = "property_id", nullable = false)
  private UUID propertyId;

  @Column(nullable = false)
  private String label;

  @Column(name = "monthly_rent", nullable = false)
  private BigDecimal monthlyRent;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UnitStatus status;

  @Column(name = "archived_at")
  private Instant archivedAt;

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

  public UUID getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(UUID propertyId) {
    this.propertyId = propertyId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public BigDecimal getMonthlyRent() {
    return monthlyRent;
  }

  public void setMonthlyRent(BigDecimal monthlyRent) {
    this.monthlyRent = monthlyRent;
  }

  public UnitStatus getStatus() {
    return status;
  }

  public void setStatus(UnitStatus status) {
    this.status = status;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(Instant archivedAt) {
    this.archivedAt = archivedAt;
  }

  public boolean isArchived() {
    return archivedAt != null;
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
