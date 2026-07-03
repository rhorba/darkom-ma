package com.darkom.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.darkom.property.dto.UnitRequest;
import com.darkom.property.entity.Property;
import com.darkom.property.entity.Unit;
import com.darkom.property.entity.UnitStatus;
import com.darkom.property.exception.PropertyNotFoundException;
import com.darkom.property.exception.UnitNotFoundException;
import com.darkom.property.repository.PropertyRepository;
import com.darkom.property.repository.UnitRepository;
import java.math.BigDecimal;
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
class UnitServiceTest {

  @Mock private UnitRepository unitRepository;
  @Mock private PropertyRepository propertyRepository;

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private UnitService unitService;

  @BeforeEach
  void setUp() {
    PropertyService propertyService = new PropertyService(propertyRepository, clock);
    unitService = new UnitService(unitRepository, propertyService, clock);
  }

  private Property accessibleProperty(UUID propertyId, UUID userId) {
    Property property = new Property();
    property.setId(propertyId);
    property.setLandlordId(userId);
    return property;
  }

  private Unit existingUnit(UUID propertyId) {
    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());
    unit.setPropertyId(propertyId);
    unit.setLabel("Apt 1");
    unit.setMonthlyRent(new BigDecimal("3500.00"));
    unit.setStatus(UnitStatus.VACANT);
    return unit;
  }

  @Test
  void listForPropertyThrowsWhenPropertyNotAccessible() {
    UUID propertyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(propertyRepository.findAccessibleById(propertyId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> unitService.listForProperty(propertyId, userId))
        .isInstanceOf(PropertyNotFoundException.class);
  }

  @Test
  void listForPropertyReturnsUnitsWhenAccessible() {
    UUID propertyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(propertyRepository.findAccessibleById(propertyId, userId))
        .thenReturn(Optional.of(accessibleProperty(propertyId, userId)));
    when(unitRepository.findAllByPropertyIdOrderByCreatedAtDesc(propertyId))
        .thenReturn(List.of(existingUnit(propertyId)));

    var result = unitService.listForProperty(propertyId, userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).label()).isEqualTo("Apt 1");
  }

  @Test
  void createDefaultsStatusToVacant() {
    UUID propertyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(propertyRepository.findAccessibleById(propertyId, userId))
        .thenReturn(Optional.of(accessibleProperty(propertyId, userId)));
    when(unitRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        unitService.create(propertyId, userId, new UnitRequest("Apt 2", new BigDecimal("4000")));

    assertThat(result.status()).isEqualTo(UnitStatus.VACANT);
    assertThat(result.archived()).isFalse();
  }

  @Test
  void createThrowsWhenPropertyNotAccessible() {
    UUID propertyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(propertyRepository.findAccessibleById(propertyId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> unitService.create(propertyId, userId, new UnitRequest("Apt 2", BigDecimal.TEN)))
        .isInstanceOf(PropertyNotFoundException.class);
  }

  @Test
  void updateThrowsWhenUnitNotFound() {
    UUID unitId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> unitService.update(unitId, userId, new UnitRequest("Apt", BigDecimal.TEN)))
        .isInstanceOf(UnitNotFoundException.class);
  }

  @Test
  void updateThrowsWhenOwningPropertyNotAccessibleToCaller() {
    UUID propertyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Unit unit = existingUnit(propertyId);
    when(unitRepository.findById(unit.getId())).thenReturn(Optional.of(unit));
    when(propertyRepository.findAccessibleById(propertyId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> unitService.update(unit.getId(), userId, new UnitRequest("Apt", BigDecimal.TEN)))
        .isInstanceOf(PropertyNotFoundException.class);
  }

  @Test
  void updateAppliesChangesWhenAccessible() {
    UUID propertyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Unit unit = existingUnit(propertyId);
    when(unitRepository.findById(unit.getId())).thenReturn(Optional.of(unit));
    when(propertyRepository.findAccessibleById(propertyId, userId))
        .thenReturn(Optional.of(accessibleProperty(propertyId, userId)));
    when(unitRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        unitService.update(
            unit.getId(), userId, new UnitRequest("Renamed", new BigDecimal("5000")));

    assertThat(result.label()).isEqualTo("Renamed");
    assertThat(result.monthlyRent()).isEqualByComparingTo("5000");
  }

  @Test
  void archiveSetsArchivedTrue() {
    UUID propertyId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Unit unit = existingUnit(propertyId);
    when(unitRepository.findById(unit.getId())).thenReturn(Optional.of(unit));
    when(propertyRepository.findAccessibleById(propertyId, userId))
        .thenReturn(Optional.of(accessibleProperty(propertyId, userId)));
    when(unitRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = unitService.archive(unit.getId(), userId);

    assertThat(result.archived()).isTrue();
    verify(unitRepository).save(unit);
  }
}
