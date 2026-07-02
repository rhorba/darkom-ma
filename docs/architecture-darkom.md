# Architecture: Darkom.ma
**PRD Reference**: docs/prd-darkom.md
**System Design Reference**: docs/system-design-darkom.md
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: Software Architect

## 1. Overview
A modular-monolith Spring Boot 3 (Java 21) API backing an Angular SPA, with PostgreSQL as the single datastore. Modules are package-by-feature inside one deployable, keeping a clean path to extraction later without paying microservice costs now.

## 2. Architecture Decision Records

### ADR-1: Angular (standalone components) for the frontend
- **Context**: User specified Angular explicitly, replacing the prior Next.js choice.
- **Decision**: Angular (latest stable, v19+), standalone components (no NgModules), Angular Router, Angular Signals for local state, Angular HttpClient for API calls.
- **Alternatives**: NgModule-based Angular (rejected — standalone is the current idiomatic default, less boilerplate).
- **Consequences**: No SSR by default (SPA served as static build via Nginx). Fine for an authenticated dashboard app — SEO isn't a requirement here (behind login).

### ADR-2: Spring Boot 3.x on Java 21 for the backend
- **Context**: User specified Java 21 + Spring Boot explicitly, replacing the prior Node/Next.js API routes.
- **Decision**: Spring Boot 3.3+ (Java 21 baseline), Spring Web (REST), Spring Data JPA, Spring Security.
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

## 5. API Design
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | /api/v1/auth/login | Authenticate, issue JWT | Public |
| POST | /api/v1/auth/refresh | Refresh access token | Refresh cookie |
| GET | /api/v1/properties | List landlord's properties | Landlord/PM |
| POST | /api/v1/properties | Create property | Landlord/PM |
| GET | /api/v1/properties/:id/units | List units for a property | Landlord/PM |
| POST | /api/v1/leases | Create lease (generates PDF) | Landlord/PM |
| GET | /api/v1/leases/:id | Get lease detail | Owner (Landlord/Tenant on that lease) |
| GET | /api/v1/leases/:id/document | Download lease PDF | Owner |
| POST | /api/v1/payments/:leaseId/initiate | Start CMI payment session | Tenant (own lease) |
| POST | /api/v1/payments/cmi/callback | CMI server-to-server callback | CMI (signature-verified, not user auth) |
| POST | /api/v1/maintenance | Submit maintenance request | Tenant |
| PATCH | /api/v1/maintenance/:id | Update request status | Landlord/PM |
| GET | /api/v1/admin/users | List all users | Admin |

## 6. Security Considerations
[Full detail in docs/security-darkom.md — summary here]
- Authentication: JWT (short-lived access + HttpOnly refresh cookie), Spring Security.
- Authorization: Role + resource-ownership checks (e.g., a Tenant can only see their own lease) enforced at the service layer, not just annotations.
- Data protection: TLS everywhere; CMI card data never touches our backend (redirect/tokenized flow); passwords hashed with BCrypt.
- Key risks: CMI callback endpoint must verify CMI's signature to prevent forged "payment succeeded" calls — flagged as a must-have, not optional.

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
