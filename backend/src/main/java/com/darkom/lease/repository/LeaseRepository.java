package com.darkom.lease.repository;

import com.darkom.lease.entity.Lease;
import com.darkom.lease.entity.LeaseStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {

  List<Lease> findAllByStatus(LeaseStatus status);
}
