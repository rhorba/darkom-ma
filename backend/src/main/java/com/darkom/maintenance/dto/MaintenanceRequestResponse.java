package com.darkom.maintenance.dto;

import com.darkom.maintenance.entity.MaintenanceRequest;
import com.darkom.maintenance.entity.MaintenanceRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record MaintenanceRequestResponse(
    UUID id,
    UUID unitId,
    UUID reportedBy,
    String description,
    boolean hasPhoto,
    MaintenanceRequestStatus status,
    Instant createdAt,
    Instant updatedAt) {

  public static MaintenanceRequestResponse from(MaintenanceRequest request) {
    return new MaintenanceRequestResponse(
        request.getId(),
        request.getUnitId(),
        request.getReportedBy(),
        request.getDescription(),
        request.getPhotoPath() != null,
        request.getStatus(),
        request.getCreatedAt(),
        request.getUpdatedAt());
  }
}
