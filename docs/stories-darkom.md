# Stories: Darkom.ma
**PRD**: docs/prd-darkom.md
**Architecture**: docs/architecture-darkom.md

## Epic 0: Scaffolding
Get an empty-but-wired Angular + Spring Boot + Postgres stack running on localhost via Docker Compose, with CI passing on an empty test suite.

### Story 0.1: Backend skeleton
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev
**Description**: As a developer, I want a Spring Boot 3/Java 21 project with the package-by-feature structure from the architecture doc, so subsequent stories have a place to live.
**Acceptance Criteria**:
```gherkin
Given the backend project is built
When I run it with `./mvnw spring-boot:run`
Then `/actuator/health` returns 200 OK
```
**Technical Notes**: Package structure per ADR-5. Flyway configured, pointing at V1 migration from docs/database-darkom.md.
**Dependencies**: none

### Story 0.2: Frontend skeleton
**Priority**: Must | **Size**: S | **Specialist**: Frontend Dev
**Description**: As a developer, I want an Angular standalone-components app with routing and Angular Material configured, so subsequent stories have a place to live.
**Acceptance Criteria**:
```gherkin
Given the frontend project is built
When I run `ng serve`
Then a placeholder home route renders with the Material theme from docs/ui-darkom.md applied
```
**Dependencies**: none

### Story 0.3: Docker Compose wiring
**Priority**: Must | **Size**: S | **Specialist**: DevOps
**Description**: As a developer, I want `docker compose up` to start frontend, backend, and Postgres together, so the whole stack runs on localhost with one command.
**Acceptance Criteria**:
```gherkin
Given .env is populated from .env.example
When I run `docker compose up`
Then frontend is reachable on :4200, backend health check passes on :8080, and backend connects to db
```
**Technical Notes**: Uses docker-compose.yml from docs/devops-darkom.md §5.
**Dependencies**: 0.1, 0.2

### Story 0.4: CI pipeline skeleton
**Priority**: Should | **Size**: S | **Specialist**: DevOps/DevSecOps
**Description**: As a developer, I want CI running lint + test + security-scan + build on every push, so quality gates exist from day one.
**Technical Notes**: Per docs/devops-darkom.md §2. Coverage gate configured but trivially passes on an empty suite for now — becomes meaningful once Epic 1 lands.
**Dependencies**: 0.1, 0.2

## Epic 1: Auth & Roles
Landlord/Tenant/PM/Admin can register (or be invited) and log in with role-scoped access.

### Story 1.1: User registration & login
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
**Description**: As a user, I want to register and log in, so I can access my role-scoped area.
**Acceptance Criteria**:
```gherkin
Given valid registration details
When I submit the registration form
Then a user is created with hashed password and I receive a JWT on login

Given invalid credentials
When I attempt login
Then I receive 401 and no token
```
**Technical Notes**: FR-1, ADR-3 (JWT), Security doc §3-4. `POST /api/v1/auth/login`.
**Dependencies**: 0.1, 0.3

### Story 1.2: Role-based route guards (frontend)
**Priority**: Must | **Size**: S | **Specialist**: Frontend Dev
**Description**: As a user, I want to only see the area matching my role, so I don't see irrelevant/unauthorized UI.
**Dependencies**: 1.1, 0.2

## Epic 2: Property & Lease Management
### Story 2.1: Property & unit CRUD
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**: FR-2, per UX Flow "Landlord creates a lease" (property/unit portion).
**Dependencies**: 1.1, 1.2

### Story 2.2: Lease creation + PDF generation
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev
**Acceptance Criteria**: FR-3, FR-4, ATDD scenarios "Lease creation" in docs/test-strategy-darkom.md.
**Technical Notes**: Double-booking prevention (adversarial checklist item) must be covered by a DB constraint or transactional check, not just app-layer validation.
**Dependencies**: 2.1

### Story 2.3: Lease creation UI
**Priority**: Must | **Size**: M | **Specialist**: Frontend Dev
**Description**: As a Landlord/PM, I want a form to create a lease for a vacant unit and see the generated PDF, so I don't have to call the API by hand (Story 2.2 shipped backend-only).
**Acceptance Criteria**:
```gherkin
Given a vacant unit on a property I manage
When I submit a lease with tenant email, start date, end date, and rent
Then the lease is created and I can download the generated PDF
And the unit's status updates to OCCUPIED without a page reload

Given a unit that already has an ACTIVE lease, or an invalid/non-tenant email, or end date before start date
When I submit the lease form
Then I see an inline validation error and no navigation away from the form
```
**Technical Notes**: Matches UX Flow 1 "Landlord creates a lease" (docs/ux-darkom.md §3). Entry point: a "Créer un bail" action per vacant unit row on the property detail page (`features/landlord/properties/property-detail`). Calls `POST /api/v1/leases`, `GET /api/v1/leases/:id/document` (existing backend from Story 2.2) - surface 409 (already leased) and 400 (bad dates/unknown tenant) as inline form errors, not generic toasts.
**Dependencies**: 2.2

## Epic 3: Payments
### Story 3.1: CMI payment initiation + callback
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Security
**Acceptance Criteria**: FR-6, ATDD scenarios "Rent payment" in docs/test-strategy-darkom.md — including the forged-callback rejection scenario.
**Technical Notes**: Signature verification is a hard requirement per Security doc §2 (Tampering row).
**Dependencies**: 2.2

### Story 3.2: Automated rent reminders
**Priority**: Should | **Size**: M | **Specialist**: Backend Dev
**Acceptance Criteria**: FR-5. Scheduled job per SDR-3.
**Dependencies**: 3.1

## Epic 4: Tenant Self-Service & Maintenance
### Story 4.1: Tenant portal (lease view, payment history)
**Priority**: Must | **Size**: M | **Specialist**: Frontend Dev
**Dependencies**: 2.2, 3.1

### Story 4.2: Maintenance requests
**Priority**: Should | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**: FR-7.
**Dependencies**: 1.2

## Epic 5: Admin & PM
### Story 5.1: Property Manager delegated access
**Priority**: Could | **Size**: M | **Specialist**: Backend Dev
**Acceptance Criteria**: FR-8.
**Dependencies**: 2.1

### Story 5.2: Admin user list + deactivation
**Priority**: Could | **Size**: S | **Specialist**: Backend Dev + Frontend Dev
**Acceptance Criteria**: FR-9.
**Dependencies**: 1.1

## Sprint Allocation
| Sprint | Stories | Estimated Effort |
|---|---|---|
| Sprint 1 | 0.1, 0.2, 0.3, 0.4 | Scaffolding, ~1 session |
| Sprint 2 | 1.1, 1.2, 2.1 | Auth + property/unit CRUD |
| Sprint 3 | 2.2, 3.1 | Lease creation + payments (highest risk, most testing) |
| Sprint 4 | 3.2, 4.1, 4.2 | Reminders + tenant portal + maintenance |
| Sprint 5 | 5.1, 5.2 | PM delegation + admin (lowest priority, cut first if time-constrained) |
