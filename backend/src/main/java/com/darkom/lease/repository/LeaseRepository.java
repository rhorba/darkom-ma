package com.darkom.lease.repository;

import com.darkom.lease.entity.Lease;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseRepository extends JpaRepository<Lease, UUID> {}
