package com.darkom.maintenance.dto;

import com.darkom.maintenance.entity.MaintenanceRequestStatus;
import jakarta.validation.constraints.NotNull;

public record MaintenanceStatusUpdateRequest(@NotNull MaintenanceRequestStatus status) {}
