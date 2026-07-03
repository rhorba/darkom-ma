package com.darkom.lease.dto;

import com.darkom.lease.entity.Lease;
import com.darkom.lease.entity.LeaseStatus;
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
    LeaseStatus status) {

  public static LeaseResponse from(Lease lease) {
    return new LeaseResponse(
        lease.getId(),
        lease.getUnitId(),
        lease.getTenantId(),
        lease.getStartDate(),
        lease.getEndDate(),
        lease.getMonthlyRent(),
        lease.getStatus());
  }
}
