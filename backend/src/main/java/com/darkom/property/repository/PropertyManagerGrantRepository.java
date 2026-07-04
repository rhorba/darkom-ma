package com.darkom.property.repository;

import com.darkom.property.entity.PropertyManagerGrant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyManagerGrantRepository
    extends JpaRepository<PropertyManagerGrant, PropertyManagerGrant.Key> {

  List<PropertyManagerGrant> findAllByPropertyId(UUID propertyId);

  boolean existsByPropertyIdAndManagerId(UUID propertyId, UUID managerId);

  void deleteByPropertyIdAndManagerId(UUID propertyId, UUID managerId);
}
