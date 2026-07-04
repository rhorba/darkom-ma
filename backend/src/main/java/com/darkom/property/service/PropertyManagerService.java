package com.darkom.property.service;

import com.darkom.auth.dto.UserSummary;
import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.property.entity.Property;
import com.darkom.property.entity.PropertyManagerGrant;
import com.darkom.property.exception.ManagerNotFoundException;
import com.darkom.property.exception.PropertyNotFoundException;
import com.darkom.property.repository.PropertyManagerGrantRepository;
import com.darkom.property.repository.PropertyRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grants/revokes are deliberately Landlord-owner-only, not the general Landlord-or-PM access rule
 * PropertyService uses elsewhere - an already-delegated PM granting further PM access would be an
 * escalation the Landlord never approved (FR-8 implies the Landlord is the one delegating).
 */
@Service
public class PropertyManagerService {

  private final PropertyRepository propertyRepository;
  private final PropertyManagerGrantRepository grantRepository;
  private final UserRepository userRepository;
  private final PropertyService propertyService;
  private final Clock clock;

  public PropertyManagerService(
      PropertyRepository propertyRepository,
      PropertyManagerGrantRepository grantRepository,
      UserRepository userRepository,
      PropertyService propertyService,
      Clock clock) {
    this.propertyRepository = propertyRepository;
    this.grantRepository = grantRepository;
    this.userRepository = userRepository;
    this.propertyService = propertyService;
    this.clock = clock;
  }

  @Transactional
  public void grant(UUID propertyId, UUID landlordId, String managerEmail) {
    ownedByOrThrow(propertyId, landlordId);
    User manager =
        userRepository
            .findByEmail(managerEmail)
            .filter(user -> user.getRole() == Role.PROPERTY_MANAGER)
            .orElseThrow(() -> new ManagerNotFoundException(managerEmail));

    if (grantRepository.existsByPropertyIdAndManagerId(propertyId, manager.getId())) {
      return;
    }

    PropertyManagerGrant grant = new PropertyManagerGrant();
    grant.setPropertyId(propertyId);
    grant.setManagerId(manager.getId());
    grant.setGrantedAt(clock.instant());
    grantRepository.save(grant);
  }

  @Transactional
  public void revoke(UUID propertyId, UUID landlordId, UUID managerId) {
    ownedByOrThrow(propertyId, landlordId);
    grantRepository.deleteByPropertyIdAndManagerId(propertyId, managerId);
  }

  @Transactional(readOnly = true)
  public List<UserSummary> listManagers(UUID propertyId, UUID currentUserId) {
    propertyService.accessibleOrThrow(propertyId, currentUserId);
    List<UUID> managerIds =
        grantRepository.findAllByPropertyId(propertyId).stream()
            .map(PropertyManagerGrant::getManagerId)
            .toList();
    return userRepository.findAllById(managerIds).stream().map(UserSummary::from).toList();
  }

  private Property ownedByOrThrow(UUID propertyId, UUID landlordId) {
    Property property =
        propertyRepository
            .findById(propertyId)
            .orElseThrow(() -> new PropertyNotFoundException(propertyId));
    if (!property.getLandlordId().equals(landlordId)) {
      throw new PropertyNotFoundException(propertyId);
    }
    return property;
  }
}
