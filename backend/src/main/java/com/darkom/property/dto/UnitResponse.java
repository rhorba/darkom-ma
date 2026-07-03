package com.darkom.property.dto;

import com.darkom.property.entity.Unit;
import com.darkom.property.entity.UnitStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record UnitResponse(
    UUID id,
    UUID propertyId,
    String label,
    BigDecimal monthlyRent,
    UnitStatus status,
    boolean archived) {

  public static UnitResponse from(Unit unit) {
    return new UnitResponse(
        unit.getId(),
        unit.getPropertyId(),
        unit.getLabel(),
        unit.getMonthlyRent(),
        unit.getStatus(),
        unit.isArchived());
  }
}
