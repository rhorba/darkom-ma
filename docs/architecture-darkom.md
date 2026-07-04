# Architecture: Darkom.ma
**PRD Reference**: docs/prd-darkom.md
**System Design Reference**: docs/system-design-darkom.md
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: Software Architect

## 1. Overview
A modular-monolith Spring Boot 4 (Java 21) API backing an Angular SPA, with PostgreSQL as the single datastore. Modules are package-by-feature inside one deployable, keeping a clean path to extraction later without paying microservice costs now.

## 2. Architecture Decision Records

### ADR-1: Angular (standalone components) for the frontend
- **Context**: User specified Angular explicitly, replacing the prior Next.js choice.
- **Decision**: Angular (latest stable, v19+), standalone components (no NgModules), Angular Router, Angular Signals for local state, Angular HttpClient for API calls.
- **Alternatives**: NgModule-based Angular (rejected — standalone is the current idiomatic default, less boilerplate).
- **Consequences**: No SSR by default (SPA served as static build via Nginx). Fine for an authenticated dashboard app — SEO isn't a requirement here (behind login).

### ADR-2: Spring Boot on Java 21 for the backend
- **Context**: User specified Java 21 + Spring Boot explicitly, replacing the prior Node/Next.js API routes.
- **Decision**: Spring Boot 4.1.0 (Java 21 baseline), Spring Web (REST), Spring Data JPA, Spring Security.
- **Alternatives**: Quarkus/Micronaut — rejected, Spring Boot is the de facto standard, best docs/ecosystem for a small team.
- **Consequences**: Standard layered structure per module: `controller → service → repository → entity`.

### ADR-3: JWT-based stateless auth (Spring Security)
- **Context**: Angular SPA + separate Spring Boot API means no shared server-side session by default.
- **Decision**: Spring Security with JWT bearer tokens (access token short-lived, refresh token via HttpOnly cookie). Roles: `LANDLORD`, `TENANT`, `PROPERTY_MANAGER`, `ADMIN` embedded as claims.
- **Alternatives**: Server-side sessions with a shared store (Redis) — rejected for MVP, adds infra for no current benefit; OAuth2/social login — rejected, out of scope, not requested.
- **Consequences**: API is stateless and horizontally-scalable-by-default even though we're not scaling yet (no cost to doing it right here).

### ADR-4: PostgreSQL 16 + Spring Data JPA/Hibernate
- **Context**: Relational data (users, properties, leases, payments) with real referential integrity needs (a lease MUST reference a valid unit and tenant).
- **Decision**: PostgreSQL 16, Spring Data JPA with Hibernate, Flyway for migrations.
- **Alternatives**: MongoDB — rejected, data is inherently relational; Drizzle/raw SQL — not applicable, off the table with the Java stack.
- **Consequences**: Schema-first via Flyway migration files, versioned in `backend/src/main/resources/db/migration`.

### ADR-5: Package-by-feature module boundaries
- **Context**: Need clear internal boundaries without microservice overhead (see SDR-1).
- **Decision**: `com.darkom.{auth, property, lease, payment, document, notification, maintenance, user}` — each with its own controller/service/repository/entity/dto. Shared code in `com.darkom.common`.
- **Alternatives**: Package-by-layer (`controllers/`, `services/` at top level) — rejected, harder to reason about feature boundaries and to extract a module later if ever needed.
- **Consequences**: Cross-module calls go through service interfaces, not repositories directly — keeps a seam for future extraction.

## 3. System Design
```
[Angular SPA] ── REST/JSON, JWT bearer ──► [Spring Boot API]
                                                  │
                          ┌───────────────────────┼───────────────────────┐
                          ▼                       ▼                       ▼
                  [PostgreSQL 16]          [CMI Gateway]           [Email Provider]
                  (Flyway-migrated)         (redirect + webhook)    (transactional API)
```

## 4. Data Model (high-level — DBA owns full schema)
```
User ──1:N──> Property (as Landlord)
Property ──1:N──> Unit
Unit ──1:N──> Lease
Lease ──N:1──> User (as Tenant)
Lease ──1:N──> Payment
Lease ──1:1──> LeaseDocument (generated PDF)
Unit ──1:N──> MaintenanceRequest ──N:1──> User (as Tenant, reporter)
User ──N:N──> Property (as Property Manager, via property_managers join table)
```

*As-built note: `payments.reminder_sent_at` (V5 migration) tracks whether Story 3.2's daily reminder job has already emailed a tenant for a given payment, so it isn't re-sent every day the window is open.*

## 5. API Design
As-built, grouped by module (`com.darkom.{auth, property, lease, payment, maintenance, user}`; see ADR-5).

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | /api/v1/auth/register | Create an account (LANDLORD/TENANT/PROPERTY_MANAGER only - no ADMIN self-registration by design) | Public |
| POST | /api/v1/auth/login | Authenticate, issue JWT | Public |
| POST | /api/v1/auth/refresh | Refresh access token | Refresh cookie |
| GET | /api/v1/properties | List accessible properties | Landlord/PM |
| POST | /api/v1/properties | Create property | Landlord |
| GET | /api/v1/properties/:id | Get property detail | Landlord/PM (owner or granted) |
| PATCH | /api/v1/properties/:id | Update property | Landlord/PM (owner or granted) |
| POST | /api/v1/properties/:id/archive | Archive property | Landlord/PM (owner or granted) |
| GET | /api/v1/properties/:id/units | List units for a property | Landlord/PM |
| POST | /api/v1/properties/:id/units | Create unit | Landlord/PM |
| GET | /api/v1/properties/:id/managers | List a property's delegated PMs | Landlord/PM (owner or granted) |
| POST | /api/v1/properties/:id/managers | Grant a PM access to a property (Story 5.1) | Landlord (owner only, not a delegated PM) |
| DELETE | /api/v1/properties/:id/managers/:managerId | Revoke a PM's access | Landlord (owner only) |
| PATCH | /api/v1/units/:id | Update unit | Landlord/PM |
| POST | /api/v1/units/:id/archive | Archive unit | Landlord/PM |
| POST | /api/v1/leases | Create lease (generates PDF, creates the first PENDING payment) | Landlord/PM |
| GET | /api/v1/leases/mine | Tenant's current active lease | Tenant |
| GET | /api/v1/leases/:id | Get lease detail | Owner (Landlord/PM/Tenant on that lease), Admin |
| GET | /api/v1/leases/:id/payments | Payment history for a lease | Same access as lease detail |
| GET | /api/v1/leases/:id/document | Download lease PDF | Same access as lease detail |
| POST | /api/v1/payments/:leaseId/initiate | Start a CMI payment session | Tenant (own lease) |
| POST | /api/v1/payments/cmi/callback | CMI server-to-server callback | CMI (signature-verified, not user auth) |
| GET | /mock-cmi/pay/:transactionId | Mock CMI hosted payment page (Story 3.1 - no real CMI merchant account exists; see SDR-3) | Public (stands in for an external bank page) |
| POST | /mock-cmi/pay/:transactionId/succeed, /fail | Mock CMI simulates a bank webhook back to our own callback | Public |
| POST | /api/v1/maintenance | Submit maintenance request (multipart: description + optional photo) | Tenant (must hold an active lease on the unit) |
| GET | /api/v1/maintenance/mine | Tenant's own maintenance requests | Tenant |
| GET | /api/v1/maintenance | Landlord/PM's maintenance inbox | Landlord/PM |
| PATCH | /api/v1/maintenance/:id | Update request status | Landlord/PM (deliberately not the reporting Tenant, not Admin) |
| GET | /api/v1/maintenance/:id/photo | Download the attached photo | Reporting Tenant, owning Landlord/PM, or Admin |
| GET | /api/v1/admin/users | List all users | Admin |
| PATCH | /api/v1/admin/users/:id/deactivate | Deactivate a user (blocks future login) | Admin |

**Internal, not client-facing**: a daily `@Scheduled` job (`RentReminderJob`, `com.darkom.notification`) generates each lease's next monthly payment and emails (mocked - `MockEmailSender`) a reminder once a payment nears its due date. No real transactional email provider is configured yet (`EMAIL_API_KEY` is a placeholder).

## 6. Security Considerations
[Full detail in docs/security-darkom.md — summary here]
- Authentication: JWT (short-lived access + HttpOnly refresh cookie), Spring Security.
- Authorization: Role + resource-ownership checks (e.g., a Tenant can only see their own lease) enforced at the service layer, not just annotations.
- Data protection: TLS everywhere; CMI card data never touches our backend (redirect/tokenized flow); passwords hashed with BCrypt.
- Key risks: CMI callback endpoint must verify CMI's signature to prevent forged "payment succeeded" calls — flagged as a must-have, not optional.
- **Gotcha for future SecurityConfig changes**: `/error` must stay in the `permitAll()` list. Spring Boot's default error handling forwards a rejected request internally to `GET /error` to render the JSON error body; without that path permitted, the forward is re-evaluated as an anonymous request and its `AuthenticationEntryPoint` silently overwrites a correct 403 with 401. MockMvc-based tests never catch this (see `SecurityErrorResponseTest`, which uses a real embedded server specifically because of it).

## 7. Infrastructure
- Hosting: Local Docker Compose (localhost) for MVP — see docs/devops-darkom.md.
- Database: PostgreSQL 16, containerized, named volume for persistence.
- CI/CD: GitHub Actions (lint, test, build, security scan) — see docs/devops-darkom.md.
- Monitoring: Spring Boot Actuator health endpoint for MVP; no external monitoring stack yet (YAGNI).

## 8. Technical Risks
| Risk | Mitigation | Owner |
|---|---|---|
| CMI callback forgery (fake payment confirmation) | Verify CMI signature/HMAC on every callback before marking a payment paid | Backend Dev + Security |
| JWT refresh token theft via XSS | Refresh token in HttpOnly, Secure, SameSite cookie; access token short-lived in memory only (not localStorage) | Frontend Dev + Security |
| Lease PDF generation drifting from legal requirements | Template versioned and reviewed before enabling in production (see PRD risk) | PM |
