package com.darkom.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyManagerServiceTest {

  @Mock private PropertyRepository propertyRepository;
  @Mock private PropertyManagerGrantRepository grantRepository;
  @Mock private UserRepository userRepository;
  @Mock private PropertyService propertyService;

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private PropertyManagerService service;

  @BeforeEach
  void setUp() {
    service =
        new PropertyManagerService(
            propertyRepository, grantRepository, userRepository, propertyService, clock);
  }

  private Property property(UUID id, UUID landlordId) {
    Property property = new Property();
    property.setId(id);
    property.setLandlordId(landlordId);
    return property;
  }

  private User manager(UUID id, Role role) {
    User user = new User();
    user.setId(id);
    user.setRole(role);
    user.setEmail("pm@example.com");
    user.setFullName("PM User");
    return user;
  }

  @Test
  void grantSucceedsForTheOwningLandlord() {
    UUID propertyId = UUID.randomUUID();
    UUID landlordId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    when(propertyRepository.findById(propertyId))
        .thenReturn(Optional.of(property(propertyId, landlordId)));
    when(userRepository.findByEmail("pm@example.com"))
        .thenReturn(Optional.of(manager(managerId, Role.PROPERTY_MANAGER)));
    when(grantRepository.existsByPropertyIdAndManagerId(propertyId, managerId)).thenReturn(false);

    service.grant(propertyId, landlordId, "pm@example.com");

    verify(grantRepository).save(any(PropertyManagerGrant.class));
  }

  @Test
  void grantIsIdempotentWhenAlreadyGranted() {
    UUID propertyId = UUID.randomUUID();
    UUID landlordId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    when(propertyRepository.findById(propertyId))
        .thenReturn(Optional.of(property(propertyId, landlordId)));
    when(userRepository.findByEmail("pm@example.com"))
        .thenReturn(Optional.of(manager(managerId, Role.PROPERTY_MANAGER)));
    when(grantRepository.existsByPropertyIdAndManagerId(propertyId, managerId)).thenReturn(true);

    service.grant(propertyId, landlordId, "pm@example.com");

    verify(grantRepository, never()).save(any());
  }

  @Test
  void grantThrowsWhenCallerIsNotTheOwningLandlord() {
    UUID propertyId = UUID.randomUUID();
    UUID actualLandlordId = UUID.randomUUID();
    UUID strangerId = UUID.randomUUID();
    when(propertyRepository.findById(propertyId))
        .thenReturn(Optional.of(property(propertyId, actualLandlordId)));

    assertThatThrownBy(() -> service.grant(propertyId, strangerId, "pm@example.com"))
        .isInstanceOf(PropertyNotFoundException.class);
  }

  @Test
  void grantThrowsWhenEmailIsNotAPropertyManager() {
    UUID propertyId = UUID.randomUUID();
    UUID landlordId = UUID.randomUUID();
    when(propertyRepository.findById(propertyId))
        .thenReturn(Optional.of(property(propertyId, landlordId)));
    when(userRepository.findByEmail("landlord2@example.com"))
        .thenReturn(Optional.of(manager(UUID.randomUUID(), Role.LANDLORD)));

    assertThatThrownBy(() -> service.grant(propertyId, landlordId, "landlord2@example.com"))
        .isInstanceOf(ManagerNotFoundException.class);
  }

  @Test
  void grantThrowsWhenEmailDoesNotExist() {
    UUID propertyId = UUID.randomUUID();
    UUID landlordId = UUID.randomUUID();
    when(propertyRepository.findById(propertyId))
        .thenReturn(Optional.of(property(propertyId, landlordId)));
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.grant(propertyId, landlordId, "nobody@example.com"))
        .isInstanceOf(ManagerNotFoundException.class);
  }

  @Test
  void revokeSucceedsForTheOwningLandlord() {
    UUID propertyId = UUID.randomUUID();
    UUID landlordId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    when(propertyRepository.findById(propertyId))
        .thenReturn(Optional.of(property(propertyId, landlordId)));

    service.revoke(propertyId, landlordId, managerId);

    verify(grantRepository).deleteByPropertyIdAndManagerId(propertyId, managerId);
  }

  @Test
  void revokeThrowsWhenCallerIsNotTheOwningLandlord() {
    UUID propertyId = UUID.randomUUID();
    UUID actualLandlordId = UUID.randomUUID();
    UUID strangerId = UUID.randomUUID();
    when(propertyRepository.findById(propertyId))
        .thenReturn(Optional.of(property(propertyId, actualLandlordId)));

    assertThatThrownBy(() -> service.revoke(propertyId, strangerId, UUID.randomUUID()))
        .isInstanceOf(PropertyNotFoundException.class);
    verify(grantRepository, never()).deleteByPropertyIdAndManagerId(any(), any());
  }

  @Test
  void listManagersReturnsGrantedManagersForAnAccessibleCaller() {
    UUID propertyId = UUID.randomUUID();
    UUID callerId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();
    PropertyManagerGrant grant = new PropertyManagerGrant();
    grant.setPropertyId(propertyId);
    grant.setManagerId(managerId);

    when(grantRepository.findAllByPropertyId(propertyId)).thenReturn(List.of(grant));
    when(userRepository.findAllById(List.of(managerId)))
        .thenReturn(List.of(manager(managerId, Role.PROPERTY_MANAGER)));

    var result = service.listManagers(propertyId, callerId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).id()).isEqualTo(managerId);
  }
}
