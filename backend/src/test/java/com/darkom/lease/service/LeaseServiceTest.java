package com.darkom.lease.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.lease.dto.LeaseRequest;
import com.darkom.lease.entity.Lease;
import com.darkom.lease.entity.LeaseStatus;
import com.darkom.lease.exception.InvalidLeaseDatesException;
import com.darkom.lease.exception.LeaseNotFoundException;
import com.darkom.lease.exception.TenantNotFoundException;
import com.darkom.lease.exception.UnitAlreadyLeasedException;
import com.darkom.lease.repository.LeaseDocumentRepository;
import com.darkom.lease.repository.LeaseRepository;
import com.darkom.property.dto.PropertyResponse;
import com.darkom.property.dto.UnitResponse;
import com.darkom.property.entity.UnitStatus;
import com.darkom.property.exception.PropertyNotFoundException;
import com.darkom.property.service.PropertyService;
import com.darkom.property.service.UnitService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class LeaseServiceTest {

  @Mock private LeaseRepository leaseRepository;
  @Mock private LeaseDocumentRepository leaseDocumentRepository;
  @Mock private UnitService unitService;
  @Mock private PropertyService propertyService;
  @Mock private UserRepository userRepository;
  @Mock private LeasePdfService leasePdfService;

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private LeaseService leaseService;

  @BeforeEach
  void setUp() {
    leaseService =
        new LeaseService(
            leaseRepository,
            leaseDocumentRepository,
            unitService,
            propertyService,
            userRepository,
            leasePdfService,
            clock);
  }

  private LeaseRequest request(UUID unitId, String tenantEmail, LocalDate start, LocalDate end) {
    return new LeaseRequest(unitId, tenantEmail, start, end, new BigDecimal("3500.00"));
  }

  private UnitResponse unit(UUID unitId, UUID propertyId) {
    return new UnitResponse(
        unitId, propertyId, "Apt 1", new BigDecimal("3500.00"), UnitStatus.VACANT, false);
  }

  private PropertyResponse property(UUID propertyId, UUID landlordId) {
    return new PropertyResponse(propertyId, landlordId, "Villa Zaytouna", "Addr", "Rabat", false);
  }

  private User user(UUID id, Role role, String email, String fullName) {
    User user = new User();
    user.setId(id);
    user.setRole(role);
    user.setEmail(email);
    user.setFullName(fullName);
    return user;
  }

  @Test
  void createRejectsEndDateBeforeStartDate() {
    UUID unitId = UUID.randomUUID();
    var req =
        request(unitId, "tenant@example.com", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1));

    assertThatThrownBy(() -> leaseService.create(UUID.randomUUID(), req))
        .isInstanceOf(InvalidLeaseDatesException.class);
  }

  @Test
  void createRejectsEqualStartAndEndDate() {
    UUID unitId = UUID.randomUUID();
    LocalDate sameDay = LocalDate.of(2026, 6, 1);
    var req = request(unitId, "tenant@example.com", sameDay, sameDay);

    assertThatThrownBy(() -> leaseService.create(UUID.randomUUID(), req))
        .isInstanceOf(InvalidLeaseDatesException.class);
  }

  @Test
  void createRejectsUnknownTenantEmail() {
    UUID currentUserId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    var req =
        request(
            unitId, "notenant@example.com", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    when(unitService.get(unitId, currentUserId)).thenReturn(unit(unitId, propertyId));
    when(propertyService.get(propertyId, currentUserId))
        .thenReturn(property(propertyId, UUID.randomUUID()));
    when(userRepository.findByEmail("notenant@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> leaseService.create(currentUserId, req))
        .isInstanceOf(TenantNotFoundException.class);
  }

  @Test
  void createRejectsEmailBelongingToNonTenantUser() {
    UUID currentUserId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    var req =
        request(
            unitId, "landlord@example.com", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    when(unitService.get(unitId, currentUserId)).thenReturn(unit(unitId, propertyId));
    when(propertyService.get(propertyId, currentUserId))
        .thenReturn(property(propertyId, UUID.randomUUID()));
    when(userRepository.findByEmail("landlord@example.com"))
        .thenReturn(
            Optional.of(user(UUID.randomUUID(), Role.LANDLORD, "landlord@example.com", "L")));

    assertThatThrownBy(() -> leaseService.create(currentUserId, req))
        .isInstanceOf(TenantNotFoundException.class);
  }

  @Test
  void createSucceedsAndMarksUnitOccupiedAndGeneratesDocument() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID landlordId = UUID.randomUUID();
    var req =
        request(unitId, "tenant@example.com", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    when(unitService.get(unitId, currentUserId)).thenReturn(unit(unitId, propertyId));
    when(propertyService.get(propertyId, currentUserId))
        .thenReturn(property(propertyId, landlordId));
    when(userRepository.findByEmail("tenant@example.com"))
        .thenReturn(
            Optional.of(user(UUID.randomUUID(), Role.TENANT, "tenant@example.com", "Sara Tenant")));
    when(userRepository.findById(landlordId))
        .thenReturn(Optional.of(user(landlordId, Role.LANDLORD, "landlord@example.com", "Rachid")));
    when(leaseRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(leasePdfService.generate(any(), any())).thenReturn("/data/lease-documents/some.pdf");

    var response = leaseService.create(currentUserId, req);

    assertThat(response.status()).isEqualTo(LeaseStatus.ACTIVE);
    assertThat(response.unitId()).isEqualTo(unitId);
    verify(unitService).markOccupied(unitId, currentUserId);
    verify(leaseDocumentRepository).save(any());
  }

  @Test
  void createTranslatesConstraintViolationToUnitAlreadyLeased() {
    UUID currentUserId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    var req =
        request(unitId, "tenant@example.com", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    when(unitService.get(unitId, currentUserId)).thenReturn(unit(unitId, propertyId));
    when(propertyService.get(propertyId, currentUserId))
        .thenReturn(property(propertyId, UUID.randomUUID()));
    when(userRepository.findByEmail("tenant@example.com"))
        .thenReturn(
            Optional.of(user(UUID.randomUUID(), Role.TENANT, "tenant@example.com", "Sara Tenant")));
    when(userRepository.findById(any()))
        .thenReturn(Optional.of(user(UUID.randomUUID(), Role.LANDLORD, "l@example.com", "L")));
    when(leaseRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));

    assertThatThrownBy(() -> leaseService.create(currentUserId, req))
        .isInstanceOf(UnitAlreadyLeasedException.class);
  }

  @Test
  void getThrowsWhenLeaseDoesNotExist() {
    UUID leaseId = UUID.randomUUID();
    when(leaseRepository.findById(leaseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> leaseService.get(leaseId, UUID.randomUUID()))
        .isInstanceOf(LeaseNotFoundException.class);
  }

  @Test
  void getAllowsTheLeasesOwnTenant() {
    UUID leaseId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    Lease lease = new Lease();
    lease.setId(leaseId);
    lease.setUnitId(UUID.randomUUID());
    lease.setTenantId(tenantId);
    lease.setStatus(LeaseStatus.ACTIVE);
    lease.setStartDate(LocalDate.of(2026, 1, 1));
    lease.setEndDate(LocalDate.of(2026, 12, 31));
    lease.setMonthlyRent(new BigDecimal("3500.00"));

    when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));
    when(userRepository.findById(tenantId))
        .thenReturn(Optional.of(user(tenantId, Role.TENANT, "t@example.com", "T")));

    var result = leaseService.get(leaseId, tenantId);

    assertThat(result.id()).isEqualTo(leaseId);
  }

  @Test
  void getAllowsAdminPlatformWideRead() {
    UUID leaseId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    Lease lease = new Lease();
    lease.setId(leaseId);
    lease.setUnitId(UUID.randomUUID());
    lease.setTenantId(UUID.randomUUID());
    lease.setStatus(LeaseStatus.ACTIVE);
    lease.setStartDate(LocalDate.of(2026, 1, 1));
    lease.setEndDate(LocalDate.of(2026, 12, 31));
    lease.setMonthlyRent(new BigDecimal("3500.00"));

    when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));
    when(userRepository.findById(adminId))
        .thenReturn(Optional.of(user(adminId, Role.ADMIN, "admin@example.com", "Admin")));

    var result = leaseService.get(leaseId, adminId);

    assertThat(result.id()).isEqualTo(leaseId);
  }

  @Test
  void getRejectsUnrelatedUserAsNotFound() {
    UUID leaseId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID strangerId = UUID.randomUUID();
    Lease lease = new Lease();
    lease.setId(leaseId);
    lease.setUnitId(unitId);
    lease.setTenantId(UUID.randomUUID());
    lease.setStatus(LeaseStatus.ACTIVE);
    lease.setStartDate(LocalDate.of(2026, 1, 1));
    lease.setEndDate(LocalDate.of(2026, 12, 31));
    lease.setMonthlyRent(new BigDecimal("3500.00"));

    when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));
    when(userRepository.findById(strangerId))
        .thenReturn(Optional.of(user(strangerId, Role.LANDLORD, "s@example.com", "S")));
    when(unitService.get(unitId, strangerId))
        .thenThrow(new PropertyNotFoundException(UUID.randomUUID()));

    assertThatThrownBy(() -> leaseService.get(leaseId, strangerId))
        .isInstanceOf(LeaseNotFoundException.class);
  }

  @Test
  void getDocumentPathThrowsWhenNoDocumentRowExists() {
    UUID leaseId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    Lease lease = new Lease();
    lease.setId(leaseId);
    lease.setUnitId(UUID.randomUUID());
    lease.setTenantId(UUID.randomUUID());
    lease.setStatus(LeaseStatus.ACTIVE);
    lease.setStartDate(LocalDate.of(2026, 1, 1));
    lease.setEndDate(LocalDate.of(2026, 12, 31));
    lease.setMonthlyRent(new BigDecimal("3500.00"));

    when(leaseRepository.findById(leaseId)).thenReturn(Optional.of(lease));
    when(userRepository.findById(adminId))
        .thenReturn(Optional.of(user(adminId, Role.ADMIN, "admin@example.com", "Admin")));
    when(leaseDocumentRepository.findByLeaseId(leaseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> leaseService.getDocumentPath(leaseId, adminId))
        .isInstanceOf(LeaseNotFoundException.class);
  }
}
