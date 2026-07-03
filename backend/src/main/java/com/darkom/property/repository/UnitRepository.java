package com.darkom.property.repository;

import com.darkom.property.entity.Unit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

  List<Unit> findAllByPropertyIdOrderByCreatedAtDesc(UUID propertyId);
}
