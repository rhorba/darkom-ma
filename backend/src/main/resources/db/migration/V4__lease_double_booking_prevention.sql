-- Prevents two ACTIVE leases on the same unit even under concurrent requests
-- (adversarial checklist: double-booking is a DB constraint, not app-layer-only).
CREATE UNIQUE INDEX idx_leases_unit_active ON leases(unit_id) WHERE status = 'ACTIVE';
