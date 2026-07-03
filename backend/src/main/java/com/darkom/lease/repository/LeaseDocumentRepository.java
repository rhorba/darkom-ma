package com.darkom.lease.repository;

import com.darkom.lease.entity.LeaseDocument;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaseDocumentRepository extends JpaRepository<LeaseDocument, UUID> {

  Optional<LeaseDocument> findByLeaseId(UUID leaseId);
}
