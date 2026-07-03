package com.darkom.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.darkom.property.dto.PropertyRequest;
import com.darkom.property.entity.Property;
import com.darkom.property.exception.PropertyNotFoundException;
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
class PropertyServiceTest {

  @Mock private PropertyRepository propertyRepository;

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private PropertyService propertyService;

  @BeforeEach
  void setUp() {
    propertyService = new PropertyService(propertyRepository, clock);
  }

  private PropertyRequest request() {
    return new PropertyRequest("Villa Zaytouna", "12 Rue des Oliviers", "Rabat");
  }

  private Property existingProperty(UUID landlordId) {
    Property property = new Property();
    property.setId(UUID.randomUUID());
    property.setLandlordId(landlordId);
    property.setName("Villa Zaytouna");
    property.setAddress("12 Rue des Oliviers");
    property.setCity("Rabat");
    return property;
  }

  @Test
  void listAccessibleToMapsRepositoryResults() {
    UUID userId = UUID.randomUUID();
    when(propertyRepository.findAllAccessibleTo(userId))
        .thenReturn(List.of(existingProperty(userId)));

    var result = propertyService.listAccessibleTo(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("Villa Zaytouna");
  }

  @Test
  void createAssignsLandlordIdAndTimestamps() {
    UUID landlordId = UUID.randomUUID();
    when(propertyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = propertyService.create(landlordId, request());

    assertThat(result.name()).isEqualTo("Villa Zaytouna");
    assertThat(result.archived()).isFalse();
  }

  @Test
  void updateThrowsWhenNotAccessible() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(propertyRepository.findAccessibleById(id, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> propertyService.update(id, userId, request()))
        .isInstanceOf(PropertyNotFoundException.class);
  }

  @Test
  void updateAppliesChangesWhenAccessible() {
    UUID userId = UUID.randomUUID();
    Property property = existingProperty(userId);
    when(propertyRepository.findAccessibleById(property.getId(), userId))
        .thenReturn(Optional.of(property));
    when(propertyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var updated =
        propertyService.update(
            property.getId(), userId, new PropertyRequest("New Name", "New Addr", "Casablanca"));

    assertThat(updated.name()).isEqualTo("New Name");
    assertThat(updated.city()).isEqualTo("Casablanca");
  }

  @Test
  void archiveSetsArchivedTrue() {
    UUID userId = UUID.randomUUID();
    Property property = existingProperty(userId);
    when(propertyRepository.findAccessibleById(property.getId(), userId))
        .thenReturn(Optional.of(property));
    when(propertyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var archived = propertyService.archive(property.getId(), userId);

    assertThat(archived.archived()).isTrue();
  }

  @Test
  void archiveThrowsWhenNotAccessible() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(propertyRepository.findAccessibleById(id, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> propertyService.archive(id, userId))
        .isInstanceOf(PropertyNotFoundException.class);
  }

  @Test
  void getReturnsPropertyWhenAccessible() {
    UUID userId = UUID.randomUUID();
    Property property = existingProperty(userId);
    when(propertyRepository.findAccessibleById(property.getId(), userId))
        .thenReturn(Optional.of(property));

    var result = propertyService.get(property.getId(), userId);

    assertThat(result.name()).isEqualTo("Villa Zaytouna");
  }

  @Test
  void getThrowsWhenNotAccessible() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(propertyRepository.findAccessibleById(id, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> propertyService.get(id, userId))
        .isInstanceOf(PropertyNotFoundException.class);
  }
}
