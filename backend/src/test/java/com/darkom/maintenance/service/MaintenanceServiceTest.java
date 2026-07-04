package com.darkom.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.darkom.auth.entity.Role;
import com.darkom.auth.entity.User;
import com.darkom.auth.repository.UserRepository;
import com.darkom.lease.entity.LeaseStatus;
import com.darkom.lease.repository.LeaseRepository;
import com.darkom.maintenance.entity.MaintenanceRequest;
import com.darkom.maintenance.entity.MaintenanceRequestStatus;
import com.darkom.maintenance.exception.MaintenanceNotAuthorizedException;
import com.darkom.maintenance.exception.MaintenanceRequestNotFoundException;
import com.darkom.maintenance.repository.MaintenanceRequestRepository;
import com.darkom.property.dto.UnitResponse;
import com.darkom.property.entity.UnitStatus;
import com.darkom.property.exception.UnitNotFoundException;
import com.darkom.property.service.UnitService;
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
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

  @Mock private MaintenanceRequestRepository maintenanceRequestRepository;
  @Mock private LeaseRepository leaseRepository;
  @Mock private UnitService unitService;
  @Mock private UserRepository userRepository;
  @Mock private MaintenancePhotoStorage photoStorage;

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private MaintenanceService maintenanceService;

  @BeforeEach
  void setUp() {
    maintenanceService =
        new MaintenanceService(
            maintenanceRequestRepository,
            leaseRepository,
            unitService,
            userRepository,
            photoStorage,
            clock);
  }

  private UnitResponse unit(UUID unitId) {
    return new UnitResponse(
        unitId, UUID.randomUUID(), "Apt 1", new BigDecimal("3500.00"), UnitStatus.OCCUPIED, false);
  }

  private MaintenanceRequest request(UUID id, UUID unitId, UUID reportedBy, String photoPath) {
    MaintenanceRequest request = new MaintenanceRequest();
    request.setId(id);
    request.setUnitId(unitId);
    request.setReportedBy(reportedBy);
    request.setDescription("Leaking faucet");
    request.setPhotoPath(photoPath);
    request.setStatus(MaintenanceRequestStatus.OPEN);
    return request;
  }

  @Test
  void createSucceedsWhenTenantHasAnActiveLeaseOnTheUnit() {
    UUID unitId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    when(unitService.getRaw(unitId)).thenReturn(unit(unitId));
    when(leaseRepository.existsByUnitIdAndTenantIdAndStatus(unitId, tenantId, LeaseStatus.ACTIVE))
        .thenReturn(true);
    when(maintenanceRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var result = maintenanceService.create(unitId, tenantId, "Leaking faucet", null);

    assertThat(result.description()).isEqualTo("Leaking faucet");
    assertThat(result.status()).isEqualTo(MaintenanceRequestStatus.OPEN);
    assertThat(result.hasPhoto()).isFalse();
    verify(photoStorage, never()).store(any(), any());
  }

  @Test
  void createStoresThePhotoWhenProvided() {
    UUID unitId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    when(unitService.getRaw(unitId)).thenReturn(unit(unitId));
    when(leaseRepository.existsByUnitIdAndTenantIdAndStatus(unitId, tenantId, LeaseStatus.ACTIVE))
        .thenReturn(true);
    when(photoStorage.store(any(), any())).thenReturn("/data/maintenance-photos/x.jpg");
    when(maintenanceRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    MockMultipartFile photo =
        new MockMultipartFile("photo", "leak.jpg", "image/jpeg", new byte[] {1, 2, 3});
    var result = maintenanceService.create(unitId, tenantId, "Leaking faucet", photo);

    assertThat(result.hasPhoto()).isTrue();
    verify(photoStorage).store(any(), eq(photo));
  }

  @Test
  void createThrowsWhenUnitDoesNotExist() {
    UUID unitId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    when(unitService.getRaw(unitId)).thenThrow(new UnitNotFoundException(unitId));

    assertThatThrownBy(() -> maintenanceService.create(unitId, tenantId, "desc", null))
        .isInstanceOf(UnitNotFoundException.class);
  }

  @Test
  void createThrowsWhenTenantHasNoActiveLeaseOnTheUnit() {
    UUID unitId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    when(unitService.getRaw(unitId)).thenReturn(unit(unitId));
    when(leaseRepository.existsByUnitIdAndTenantIdAndStatus(unitId, tenantId, LeaseStatus.ACTIVE))
        .thenReturn(false);

    assertThatThrownBy(() -> maintenanceService.create(unitId, tenantId, "desc", null))
        .isInstanceOf(MaintenanceNotAuthorizedException.class);
    verify(maintenanceRequestRepository, never()).save(any());
  }

  @Test
  void listMineReturnsRequestsReportedByTheCaller() {
    UUID tenantId = UUID.randomUUID();
    MaintenanceRequest req = request(UUID.randomUUID(), UUID.randomUUID(), tenantId, null);
    when(maintenanceRequestRepository.findAllByReportedByOrderByCreatedAtDesc(tenantId))
        .thenReturn(List.of(req));

    var result = maintenanceService.listMine(tenantId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).reportedBy()).isEqualTo(tenantId);
  }

  @Test
  void listForLandlordReturnsRequestsAcrossOwnedProperties() {
    UUID landlordId = UUID.randomUUID();
    MaintenanceRequest req = request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);
    when(maintenanceRequestRepository.findAllAccessibleToLandlord(landlordId))
        .thenReturn(List.of(req));

    var result = maintenanceService.listForLandlord(landlordId);

    assertThat(result).hasSize(1);
  }

  @Test
  void updateStatusSucceedsForAnAccessibleLandlord() {
    UUID requestId = UUID.randomUUID();
    UUID landlordId = UUID.randomUUID();
    MaintenanceRequest req = request(requestId, UUID.randomUUID(), UUID.randomUUID(), null);
    when(maintenanceRequestRepository.findAccessibleToLandlordById(requestId, landlordId))
        .thenReturn(Optional.of(req));
    when(maintenanceRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var result =
        maintenanceService.updateStatus(
            requestId, landlordId, MaintenanceRequestStatus.IN_PROGRESS);

    assertThat(result.status()).isEqualTo(MaintenanceRequestStatus.IN_PROGRESS);
  }

  @Test
  void updateStatusThrowsWhenLandlordHasNoAccess() {
    UUID requestId = UUID.randomUUID();
    UUID strangerId = UUID.randomUUID();
    when(maintenanceRequestRepository.findAccessibleToLandlordById(requestId, strangerId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                maintenanceService.updateStatus(
                    requestId, strangerId, MaintenanceRequestStatus.RESOLVED))
        .isInstanceOf(MaintenanceRequestNotFoundException.class);
  }

  @Test
  void getPhotoPathReturnsPathForTheReportingTenant() {
    UUID requestId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    MaintenanceRequest req =
        request(requestId, UUID.randomUUID(), tenantId, "/data/maintenance-photos/x.jpg");
    when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(req));

    var result = maintenanceService.getPhotoPath(requestId, tenantId);

    assertThat(result.toString())
        .isEqualTo("/data/maintenance-photos/x.jpg".replace("/", java.io.File.separator));
  }

  @Test
  void getPhotoPathReturnsPathForAnAccessibleLandlord() {
    UUID requestId = UUID.randomUUID();
    UUID landlordId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    MaintenanceRequest req =
        request(requestId, UUID.randomUUID(), tenantId, "/data/maintenance-photos/x.jpg");
    when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(req));
    when(userRepository.findById(landlordId))
        .thenReturn(Optional.of(user(landlordId, Role.LANDLORD)));
    when(maintenanceRequestRepository.findAccessibleToLandlordById(requestId, landlordId))
        .thenReturn(Optional.of(req));

    var result = maintenanceService.getPhotoPath(requestId, landlordId);

    assertThat(result).isNotNull();
  }

  @Test
  void getPhotoPathAllowsAdminPlatformWideRead() {
    UUID requestId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    MaintenanceRequest req =
        request(requestId, UUID.randomUUID(), UUID.randomUUID(), "/data/maintenance-photos/x.jpg");
    when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(req));
    when(userRepository.findById(adminId)).thenReturn(Optional.of(user(adminId, Role.ADMIN)));

    var result = maintenanceService.getPhotoPath(requestId, adminId);

    assertThat(result).isNotNull();
  }

  @Test
  void getPhotoPathThrowsWhenNoPhotoExists() {
    UUID requestId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();
    MaintenanceRequest req = request(requestId, UUID.randomUUID(), tenantId, null);
    when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(req));

    assertThatThrownBy(() -> maintenanceService.getPhotoPath(requestId, tenantId))
        .isInstanceOf(MaintenanceRequestNotFoundException.class);
  }

  @Test
  void getPhotoPathThrowsForAnUnrelatedUser() {
    UUID requestId = UUID.randomUUID();
    UUID strangerId = UUID.randomUUID();
    MaintenanceRequest req =
        request(requestId, UUID.randomUUID(), UUID.randomUUID(), "/data/maintenance-photos/x.jpg");
    when(maintenanceRequestRepository.findById(requestId)).thenReturn(Optional.of(req));
    when(userRepository.findById(strangerId))
        .thenReturn(Optional.of(user(strangerId, Role.LANDLORD)));
    when(maintenanceRequestRepository.findAccessibleToLandlordById(requestId, strangerId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> maintenanceService.getPhotoPath(requestId, strangerId))
        .isInstanceOf(MaintenanceRequestNotFoundException.class);
  }

  private User user(UUID id, Role role) {
    User user = new User();
    user.setId(id);
    user.setRole(role);
    user.setEmail("user@example.com");
    user.setFullName("Test User");
    return user;
  }
}
