package com.darkom.property.service;

import com.darkom.property.dto.UnitRequest;
import com.darkom.property.dto.UnitResponse;
import com.darkom.property.entity.Unit;
import com.darkom.property.entity.UnitStatus;
import com.darkom.property.exception.UnitNotFoundException;
import com.darkom.property.repository.UnitRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnitService {

  private final UnitRepository unitRepository;
  private final PropertyService propertyService;
  private final Clock clock;

  public UnitService(UnitRepository unitRepository, PropertyService propertyService, Clock clock) {
    this.unitRepository = unitRepository;
    this.propertyService = propertyService;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<UnitResponse> listForProperty(UUID propertyId, UUID userId) {
    propertyService.accessibleOrThrow(propertyId, userId);
    return unitRepository.findAllByPropertyIdOrderByCreatedAtDesc(propertyId).stream()
        .map(UnitResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public UnitResponse get(UUID unitId, UUID userId) {
    return UnitResponse.from(accessibleOrThrow(unitId, userId));
  }

  /**
   * No ownership check - only safe to call once the caller's relationship to some other resource
   * (e.g. a lease) that references this unit has already been verified. Used to enrich a response
   * for a caller (like a Tenant) who wouldn't pass the normal Landlord/PM ownership check but is
   * still entitled to see this unit's basic display info in that context.
   */
  @Transactional(readOnly = true)
  public UnitResponse getRaw(UUID unitId) {
    return UnitResponse.from(
        unitRepository.findById(unitId).orElseThrow(() -> new UnitNotFoundException(unitId)));
  }

  @Transactional
  public UnitResponse create(UUID propertyId, UUID userId, UnitRequest request) {
    propertyService.accessibleOrThrow(propertyId, userId);

    Instant now = clock.instant();
    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());
    unit.setPropertyId(propertyId);
    unit.setLabel(request.label());
    unit.setMonthlyRent(request.monthlyRent());
    unit.setStatus(UnitStatus.VACANT);
    unit.setCreatedAt(now);
    unit.setUpdatedAt(now);

    return UnitResponse.from(unitRepository.save(unit));
  }

  @Transactional
  public UnitResponse update(UUID unitId, UUID userId, UnitRequest request) {
    Unit unit = accessibleOrThrow(unitId, userId);
    unit.setLabel(request.label());
    unit.setMonthlyRent(request.monthlyRent());
    unit.setUpdatedAt(clock.instant());

    return UnitResponse.from(unitRepository.save(unit));
  }

  @Transactional
  public UnitResponse archive(UUID unitId, UUID userId) {
    Unit unit = accessibleOrThrow(unitId, userId);
    unit.setArchivedAt(clock.instant());
    unit.setUpdatedAt(clock.instant());

    return UnitResponse.from(unitRepository.save(unit));
  }

  /** Called by LeaseService once a lease is created for this unit. */
  @Transactional
  public void markOccupied(UUID unitId, UUID userId) {
    Unit unit = accessibleOrThrow(unitId, userId);
    unit.setStatus(UnitStatus.OCCUPIED);
    unit.setUpdatedAt(clock.instant());
    unitRepository.save(unit);
  }

  private Unit accessibleOrThrow(UUID unitId, UUID userId) {
    Unit unit =
        unitRepository.findById(unitId).orElseThrow(() -> new UnitNotFoundException(unitId));
    propertyService.accessibleOrThrow(unit.getPropertyId(), userId);
    return unit;
  }
}
