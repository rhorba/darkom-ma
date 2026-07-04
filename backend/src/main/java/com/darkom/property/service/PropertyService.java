package com.darkom.property.service;

import com.darkom.property.dto.PropertyRequest;
import com.darkom.property.dto.PropertyResponse;
import com.darkom.property.entity.Property;
import com.darkom.property.exception.PropertyNotFoundException;
import com.darkom.property.repository.PropertyRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final Clock clock;

  public PropertyService(PropertyRepository propertyRepository, Clock clock) {
    this.propertyRepository = propertyRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<PropertyResponse> listAccessibleTo(UUID userId) {
    return propertyRepository.findAllAccessibleTo(userId).stream()
        .map(PropertyResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public PropertyResponse get(UUID id, UUID userId) {
    return PropertyResponse.from(accessibleOrThrow(id, userId));
  }

  /**
   * No ownership check - only safe to call once the caller's relationship to some other resource
   * that references this property has already been verified (see UnitService.getRaw's doc).
   */
  @Transactional(readOnly = true)
  public PropertyResponse getRaw(UUID id) {
    return PropertyResponse.from(
        propertyRepository.findById(id).orElseThrow(() -> new PropertyNotFoundException(id)));
  }

  @Transactional
  public PropertyResponse create(UUID landlordId, PropertyRequest request) {
    Instant now = clock.instant();
    Property property = new Property();
    property.setId(UUID.randomUUID());
    property.setLandlordId(landlordId);
    property.setName(request.name());
    property.setAddress(request.address());
    property.setCity(request.city());
    property.setCreatedAt(now);
    property.setUpdatedAt(now);

    return PropertyResponse.from(propertyRepository.save(property));
  }

  @Transactional
  public PropertyResponse update(UUID id, UUID userId, PropertyRequest request) {
    Property property = accessibleOrThrow(id, userId);
    property.setName(request.name());
    property.setAddress(request.address());
    property.setCity(request.city());
    property.setUpdatedAt(clock.instant());

    return PropertyResponse.from(propertyRepository.save(property));
  }

  @Transactional
  public PropertyResponse archive(UUID id, UUID userId) {
    Property property = accessibleOrThrow(id, userId);
    property.setArchivedAt(clock.instant());
    property.setUpdatedAt(clock.instant());

    return PropertyResponse.from(propertyRepository.save(property));
  }

  /** Package-private so UnitService can verify property access without duplicating the query. */
  Property accessibleOrThrow(UUID id, UUID userId) {
    return propertyRepository
        .findAccessibleById(id, userId)
        .orElseThrow(() -> new PropertyNotFoundException(id));
  }
}
