# Database Design: Darkom.ma
**Architecture Reference**: docs/architecture-darkom.md
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: DBA

## 1. Database Selection
- **Engine**: PostgreSQL 16.
- **Rationale**: Relational integrity is essential (a lease must reference a valid unit + tenant; a payment must reference a valid lease). YAGNI default for a Java/Spring stack, mature Flyway/JPA tooling.
- **Hosting**: Containerized via Docker Compose for MVP (named volume for persistence); managed Postgres (e.g. RDS) if/when deployed beyond localhost.

## 2. Entity-Relationship Model
```
users ──1:N──> properties (landlord_id)
properties ──1:N──> units
units ──1:N──> leases
users ──1:N──> leases (tenant_id)
leases ──1:N──> payments
leases ──1:1──> lease_documents
units ──1:N──> maintenance_requests
users ──1:N──> maintenance_requests (reported_by)
users ──N:N──> properties (via property_managers, for PROPERTY_MANAGER role)
```

## 3. Schema Design
```sql
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

CREATE TABLE property_managers (
  property_id   UUID NOT NULL REFERENCES properties(id),
  manager_id    UUID NOT NULL REFERENCES users(id),
  granted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (property_id, manager_id)
);

CREATE TABLE units (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  property_id   UUID NOT NULL REFERENCES properties(id),
  label         VARCHAR(100) NOT NULL,
  monthly_rent  NUMERIC(10,2) NOT NULL,
  status        VARCHAR(32) NOT NULL DEFAULT 'VACANT' CHECK (status IN ('VACANT','OCCUPIED')),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

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
```

## 4. Index Strategy
| Table | Index Name | Columns | Query Pattern |
|---|---|---|---|
| properties | idx_properties_landlord | landlord_id | List a landlord's properties |
| units | idx_units_property | property_id | List units for a property |
| leases | idx_leases_unit | unit_id | Get active lease for a unit |
| leases | idx_leases_tenant | tenant_id | Tenant's own lease(s) |
| payments | idx_payments_lease | lease_id | Payment history for a lease |
| payments | idx_payments_status_due | status, due_date | Reminder job: find PENDING payments due soon |
| maintenance_requests | idx_maintenance_unit | unit_id | Landlord view of a unit's requests |
| property_managers | idx_propmgr_manager | manager_id | A PM's list of managed properties |

*Foreign key columns get an index by convention. No composite/covering indexes beyond `payments(status, due_date)` are justified yet — add only if a query is measured slow.*

## 5. Migration Plan
| Migration File | Description | Reversible |
|---|---|---|
| V1__initial_schema.sql | Creates users, properties, property_managers, units, leases, lease_documents, payments, maintenance_requests + indexes | Yes (Flyway `undo` not used; rollback via new down-migration if ever needed) |
| V2__refresh_tokens.sql | Adds `refresh_tokens` table (SHA-256-hashed opaque tokens, DB-tracked rotation - see ADR-3/security doc) | Yes |
| V3__archive_properties_units.sql | Adds `archived_at` to `properties` and `units` (soft-archive, not hard delete) | Yes |
| V4__lease_double_booking_prevention.sql | Partial unique index `idx_leases_unit_active` on `leases(unit_id) WHERE status = 'ACTIVE'` - prevents two active leases on the same unit at the DB level, not just in application logic | Yes |
| V5__payment_reminder_tracking.sql | Adds `payments.reminder_sent_at` - without it the daily reminder job (Story 3.2) would re-email the same tenant every day the reminder window is open | Yes |

*Flyway manages migrations under `backend/src/main/resources/db/migration/`. One file per schema change going forward, never edit an already-applied migration.*

## 6. Access Patterns
| Use Case | Query Pattern | Index Coverage |
|---|---|---|
| Landlord dashboard: my properties | SELECT by landlord_id | idx_properties_landlord |
| Tenant portal: my lease | SELECT by tenant_id | idx_leases_tenant |
| Payment history for a lease | SELECT by lease_id | idx_payments_lease |
| Daily reminder job | SELECT WHERE status='PENDING' AND due_date <= ? | idx_payments_status_due |
| Landlord: maintenance requests for a unit | SELECT by unit_id | idx_maintenance_unit |

## 7. Sensitive Data
- Columns requiring encryption: none at application level for MVP (see Security doc — deferred to volume/disk encryption). If national ID (CIN) is added to `users` later, revisit column-level encryption then.
- Row-level security needed: no — enforced in the Spring Boot service layer instead (resource-ownership checks per Security doc), consistent with the modular-monolith/JPA approach. RLS would duplicate that logic in the DB for no current benefit.
