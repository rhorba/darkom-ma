package com.darkom.maintenance.service;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.lease.entity.LeaseStatus;
import com.darkom.lease.repository.LeaseRepository;
import com.darkom.maintenance.dto.MaintenanceRequestResponse;
import com.darkom.maintenance.entity.MaintenanceRequest;
import com.darkom.maintenance.entity.MaintenanceRequestStatus;
import com.darkom.maintenance.exception.MaintenanceNotAuthorizedException;
import com.darkom.maintenance.exception.MaintenanceRequestNotFoundException;
import com.darkom.maintenance.repository.MaintenanceRequestRepository;
import com.darkom.property.service.UnitService;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MaintenanceService {

  private final MaintenanceRequestRepository maintenanceRequestRepository;
  private final LeaseRepository leaseRepository;
  private final UnitService unitService;
  private final UserRepository userRepository;
  private final MaintenancePhotoStorage photoStorage;
  private final Clock clock;

  public MaintenanceService(
      MaintenanceRequestRepository maintenanceRequestRepository,
      LeaseRepository leaseRepository,
      UnitService unitService,
      UserRepository userRepository,
      MaintenancePhotoStorage photoStorage,
      Clock clock) {
    this.maintenanceRequestRepository = maintenanceRequestRepository;
    this.leaseRepository = leaseRepository;
    this.unitService = unitService;
    this.userRepository = userRepository;
    this.photoStorage = photoStorage;
    this.clock = clock;
  }

  @Transactional
  public MaintenanceRequestResponse create(
      UUID unitId, UUID tenantId, String description, MultipartFile photo) {
    unitService.getRaw(unitId); // throws UnitNotFoundException if the unit doesn't exist

    if (!leaseRepository.existsByUnitIdAndTenantIdAndStatus(unitId, tenantId, LeaseStatus.ACTIVE)) {
      throw new MaintenanceNotAuthorizedException();
    }

    Instant now = clock.instant();
    MaintenanceRequest request = new MaintenanceRequest();
    request.setId(UUID.randomUUID());
    request.setUnitId(unitId);
    request.setReportedBy(tenantId);
    request.setDescription(description);
    request.setStatus(MaintenanceRequestStatus.OPEN);
    request.setCreatedAt(now);
    request.setUpdatedAt(now);

    if (photo != null && !photo.isEmpty()) {
      request.setPhotoPath(photoStorage.store(request.getId(), photo));
    }

    return MaintenanceRequestResponse.from(maintenanceRequestRepository.save(request));
  }

  @Transactional(readOnly = true)
  public List<MaintenanceRequestResponse> listMine(UUID tenantId) {
    return maintenanceRequestRepository.findAllByReportedByOrderByCreatedAtDesc(tenantId).stream()
        .map(MaintenanceRequestResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MaintenanceRequestResponse> listForLandlord(UUID currentUserId) {
    return maintenanceRequestRepository.findAllAccessibleToLandlord(currentUserId).stream()
        .map(MaintenanceRequestResponse::from)
        .toList();
  }

  @Transactional
  public MaintenanceRequestResponse updateStatus(
      UUID requestId, UUID currentUserId, MaintenanceRequestStatus status) {
    MaintenanceRequest request =
        maintenanceRequestRepository
            .findAccessibleToLandlordById(requestId, currentUserId)
            .orElseThrow(() -> new MaintenanceRequestNotFoundException(requestId));
    request.setStatus(status);
    request.setUpdatedAt(clock.instant());
    return MaintenanceRequestResponse.from(maintenanceRequestRepository.save(request));
  }

  @Transactional(readOnly = true)
  public Path getPhotoPath(UUID requestId, UUID currentUserId) {
    MaintenanceRequest request = accessibleOrThrow(requestId, currentUserId);
    if (request.getPhotoPath() == null) {
      throw new MaintenanceRequestNotFoundException(requestId);
    }
    return Path.of(request.getPhotoPath());
  }

  private MaintenanceRequest accessibleOrThrow(UUID requestId, UUID currentUserId) {
    MaintenanceRequest request =
        maintenanceRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new MaintenanceRequestNotFoundException(requestId));

    if (request.getReportedBy().equals(currentUserId)) {
      return request;
    }

    User caller =
        userRepository
            .findById(currentUserId)
            .orElseThrow(() -> new MaintenanceRequestNotFoundException(requestId));
    if (caller.getRole() == Role.ADMIN) {
      return request;
    }

    maintenanceRequestRepository
        .findAccessibleToLandlordById(requestId, currentUserId)
        .orElseThrow(() -> new MaintenanceRequestNotFoundException(requestId));
    return request;
  }
}
