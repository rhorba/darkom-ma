package com.darkom.lease.service;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.lease.dto.LeaseRequest;
import com.darkom.lease.dto.LeaseResponse;
import com.darkom.lease.entity.Lease;
import com.darkom.lease.entity.LeaseDocument;
import com.darkom.lease.entity.LeaseStatus;
import com.darkom.lease.exception.InvalidLeaseDatesException;
import com.darkom.lease.exception.LeaseNotFoundException;
import com.darkom.lease.exception.TenantNotFoundException;
import com.darkom.lease.exception.UnitAlreadyLeasedException;
import com.darkom.lease.repository.LeaseDocumentRepository;
import com.darkom.lease.repository.LeaseRepository;
import com.darkom.property.dto.PropertyResponse;
import com.darkom.property.dto.UnitResponse;
import com.darkom.property.exception.PropertyNotFoundException;
import com.darkom.property.exception.UnitNotFoundException;
import com.darkom.property.service.PropertyService;
import com.darkom.property.service.UnitService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaseService {

  private final LeaseRepository leaseRepository;
  private final LeaseDocumentRepository leaseDocumentRepository;
  private final UnitService unitService;
  private final PropertyService propertyService;
  private final UserRepository userRepository;
  private final LeasePdfService leasePdfService;
  private final Clock clock;

  public LeaseService(
      LeaseRepository leaseRepository,
      LeaseDocumentRepository leaseDocumentRepository,
      UnitService unitService,
      PropertyService propertyService,
      UserRepository userRepository,
      LeasePdfService leasePdfService,
      Clock clock) {
    this.leaseRepository = leaseRepository;
    this.leaseDocumentRepository = leaseDocumentRepository;
    this.unitService = unitService;
    this.propertyService = propertyService;
    this.userRepository = userRepository;
    this.leasePdfService = leasePdfService;
    this.clock = clock;
  }

  @Transactional
  public LeaseResponse create(UUID currentUserId, LeaseRequest request) {
    if (!request.startDate().isBefore(request.endDate())) {
      throw new InvalidLeaseDatesException();
    }

    UnitResponse unit = unitService.get(request.unitId(), currentUserId);
    PropertyResponse property = propertyService.get(unit.propertyId(), currentUserId);

    User tenant =
        userRepository
            .findByEmail(request.tenantEmail())
            .filter(user -> user.getRole() == Role.TENANT)
            .orElseThrow(() -> new TenantNotFoundException(request.tenantEmail()));
    User landlord =
        userRepository
            .findById(property.landlordId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Property landlord missing: " + property.landlordId()));

    Instant now = clock.instant();
    Lease lease = new Lease();
    lease.setId(UUID.randomUUID());
    lease.setUnitId(unit.id());
    lease.setTenantId(tenant.getId());
    lease.setStartDate(request.startDate());
    lease.setEndDate(request.endDate());
    lease.setMonthlyRent(request.monthlyRent());
    lease.setStatus(LeaseStatus.ACTIVE);
    lease.setCreatedAt(now);
    lease.setUpdatedAt(now);

    try {
      leaseRepository.saveAndFlush(lease);
    } catch (DataIntegrityViolationException e) {
      throw new UnitAlreadyLeasedException(unit.id());
    }

    unitService.markOccupied(unit.id(), currentUserId);

    LeaseDocumentData documentData =
        new LeaseDocumentData(
            property.name(),
            property.address(),
            unit.label(),
            landlord.getFullName(),
            tenant.getFullName(),
            tenant.getEmail(),
            request.startDate(),
            request.endDate(),
            request.monthlyRent());

    String filePath;
    try {
      filePath = leasePdfService.generate(lease.getId(), documentData);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to generate lease PDF", e);
    }

    LeaseDocument document = new LeaseDocument();
    document.setId(UUID.randomUUID());
    document.setLeaseId(lease.getId());
    document.setFilePath(filePath);
    document.setTemplateVersion(LeasePdfService.TEMPLATE_VERSION);
    document.setGeneratedAt(now);
    leaseDocumentRepository.save(document);

    return LeaseResponse.from(lease);
  }

  @Transactional(readOnly = true)
  public LeaseResponse get(UUID leaseId, UUID currentUserId) {
    return LeaseResponse.from(accessibleOrThrow(leaseId, currentUserId));
  }

  @Transactional(readOnly = true)
  public Path getDocumentPath(UUID leaseId, UUID currentUserId) {
    accessibleOrThrow(leaseId, currentUserId);
    LeaseDocument document =
        leaseDocumentRepository
            .findByLeaseId(leaseId)
            .orElseThrow(() -> new LeaseNotFoundException(leaseId));
    return Path.of(document.getFilePath());
  }

  private Lease accessibleOrThrow(UUID leaseId, UUID currentUserId) {
    Lease lease =
        leaseRepository.findById(leaseId).orElseThrow(() -> new LeaseNotFoundException(leaseId));
    User caller =
        userRepository
            .findById(currentUserId)
            .orElseThrow(() -> new LeaseNotFoundException(leaseId));

    if (caller.getRole() == Role.ADMIN || lease.getTenantId().equals(currentUserId)) {
      return lease;
    }

    try {
      unitService.get(lease.getUnitId(), currentUserId);
    } catch (PropertyNotFoundException | UnitNotFoundException e) {
      throw new LeaseNotFoundException(leaseId);
    }
    return lease;
  }
}
