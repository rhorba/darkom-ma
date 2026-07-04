package com.darkom.lease.dto;

import com.darkom.lease.entity.Lease;
import com.darkom.lease.entity.LeaseStatus;
import com.darkom.property.dto.PropertyResponse;
import com.darkom.property.dto.UnitResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeaseResponse(
    UUID id,
    UUID unitId,
    UUID tenantId,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal monthlyRent,
    LeaseStatus status,
    String unitLabel,
    String propertyName,
    String propertyAddress,
    String propertyCity) {

  public static LeaseResponse from(Lease lease, UnitResponse unit, PropertyResponse property) {
    return new LeaseResponse(
        lease.getId(),
        lease.getUnitId(),
        lease.getTenantId(),
        lease.getStartDate(),
        lease.getEndDate(),
        lease.getMonthlyRent(),
        lease.getStatus(),
        unit.label(),
        property.name(),
        property.address(),
        property.city());
  }
}
