package com.darkom.property.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "property_managers")
@IdClass(PropertyManagerGrant.Key.class)
public class PropertyManagerGrant {

  @Id
  @Column(name = "property_id")
  private UUID propertyId;

  @Id
  @Column(name = "manager_id")
  private UUID managerId;

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt;

  public UUID getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(UUID propertyId) {
    this.propertyId = propertyId;
  }

  public UUID getManagerId() {
    return managerId;
  }

  public void setManagerId(UUID managerId) {
    this.managerId = managerId;
  }

  public Instant getGrantedAt() {
    return grantedAt;
  }

  public void setGrantedAt(Instant grantedAt) {
    this.grantedAt = grantedAt;
  }

  public static class Key implements Serializable {
    private UUID propertyId;
    private UUID managerId;

    public Key() {}

    public Key(UUID propertyId, UUID managerId) {
      this.propertyId = propertyId;
      this.managerId = managerId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Key key)) return false;
      return Objects.equals(propertyId, key.propertyId) && Objects.equals(managerId, key.managerId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(propertyId, managerId);
    }
  }
}
