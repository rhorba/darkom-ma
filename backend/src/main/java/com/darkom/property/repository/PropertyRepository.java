package com.darkom.property.repository;

import com.darkom.property.entity.Property;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

  @Query(
      """
      select p from Property p
      where p.landlordId = :userId
         or exists (
              select 1 from PropertyManagerGrant pm
              where pm.propertyId = p.id and pm.managerId = :userId
            )
      order by p.createdAt desc
      """)
  List<Property> findAllAccessibleTo(@Param("userId") UUID userId);

  @Query(
      """
      select p from Property p
      where p.id = :id
        and (p.landlordId = :userId
             or exists (
                  select 1 from PropertyManagerGrant pm
                  where pm.propertyId = p.id and pm.managerId = :userId
                ))
      """)
  Optional<Property> findAccessibleById(@Param("id") UUID id, @Param("userId") UUID userId);
}
