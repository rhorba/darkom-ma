CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(255) NOT NULL,
  phone         VARCHAR(32),
  role          VARCHAR(32) NOT NULL CHECK (role IN ('LANDLORD','TENANT','PROPERTY_MANAGER','ADMIN')),
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE properties (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  landlord_id   UUID NOT NULL REFERENCES users(id),
  name          VARCHAR(255) NOT NULL,
  address       TEXT NOT NULL,
  city          VARCHAR(100) NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_properties_landlord ON properties(landlord_id);

CREATE TABLE property_managers (
  property_id   UUID NOT NULL REFERENCES properties(id),
  manager_id    UUID NOT NULL REFERENCES users(id),
  granted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (property_id, manager_id)
);

CREATE INDEX idx_propmgr_manager ON property_managers(manager_id);

CREATE TABLE units (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id   UUID NOT NULL REFERENCES properties(id),
  label         VARCHAR(100) NOT NULL,
  monthly_rent  NUMERIC(10,2) NOT NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'VACANT' CHECK (status IN ('VACANT','OCCUPIED')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_units_property ON units(property_id);

CREATE TABLE leases (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  unit_id       UUID NOT NULL REFERENCES units(id),
  tenant_id     UUID NOT NULL REFERENCES users(id),
  start_date    DATE NOT NULL,
  end_date      DATE NOT NULL,
  monthly_rent  NUMERIC(10,2) NOT NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ENDED','TERMINATED')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leases_unit ON leases(unit_id);
CREATE INDEX idx_leases_tenant ON leases(tenant_id);

CREATE TABLE lease_documents (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  lease_id      UUID NOT NULL UNIQUE REFERENCES leases(id),
  file_path     VARCHAR(512) NOT NULL,
  template_version VARCHAR(32) NOT NULL,
  generated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payments (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  lease_id      UUID NOT NULL REFERENCES leases(id),
  amount        NUMERIC(10,2) NOT NULL,
  due_date      DATE NOT NULL,
  paid_at       TIMESTAMPTZ,
  status        VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PAID','OVERDUE','FAILED')),
  cmi_transaction_id VARCHAR(255),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_lease ON payments(lease_id);
CREATE INDEX idx_payments_status_due ON payments(status, due_date);

CREATE TABLE maintenance_requests (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  unit_id       UUID NOT NULL REFERENCES units(id),
  reported_by   UUID NOT NULL REFERENCES users(id),
  description   TEXT NOT NULL,
  photo_path    VARCHAR(512),
  status        VARCHAR(32) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_maintenance_unit ON maintenance_requests(unit_id);
