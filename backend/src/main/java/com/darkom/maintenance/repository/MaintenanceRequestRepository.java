package com.darkom.maintenance.repository;

import com.darkom.maintenance.entity.MaintenanceRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {

  List<MaintenanceRequest> findAllByReportedByOrderByCreatedAtDesc(UUID reportedBy);

  @Query(
      """
      select m from MaintenanceRequest m
      join Unit u on u.id = m.unitId
      join Property p on p.id = u.propertyId
      where p.landlordId = :userId
         or exists (
              select 1 from PropertyManagerGrant pm
              where pm.propertyId = p.id and pm.managerId = :userId
            )
      order by m.createdAt desc
      """)
  List<MaintenanceRequest> findAllAccessibleToLandlord(@Param("userId") UUID userId);

  @Query(
      """
      select m from MaintenanceRequest m
      join Unit u on u.id = m.unitId
      join Property p on p.id = u.propertyId
      where m.id = :id
        and (p.landlordId = :userId
             or exists (
                  select 1 from PropertyManagerGrant pm
                  where pm.propertyId = p.id and pm.managerId = :userId
                ))
      """)
  Optional<MaintenanceRequest> findAccessibleToLandlordById(
      @Param("id") UUID id, @Param("userId") UUID userId);
}
