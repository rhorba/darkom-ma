package com.darkom.property.dto;

import com.darkom.property.entity.Property;
import java.util.UUID;

public record PropertyResponse(
    UUID id, UUID landlordId, String name, String address, String city, boolean archived) {

  public static PropertyResponse from(Property property) {
    return new PropertyResponse(
        property.getId(),
        property.getLandlordId(),
        property.getName(),
        property.getAddress(),
        property.getCity(),
        property.isArchived());
  }
}
