# PRD: Darkom.ma — Property Rental Management SaaS
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: PM | **Status**: Draft — pending approval

## 1. Problem Statement
Morocco's rental market is ~90% paper-based. Landlords manage multiple properties through ad-hoc WhatsApp messages, with no affordable tool to track leases, collect rent, manage maintenance requests, or generate lease contracts compliant with Moroccan tenancy law. Tenants have no self-service way to pay rent or raise issues.

## 2. Goals & Success Metrics
| Goal | Metric | Target (MVP, first 90 days post-launch) |
|---|---|---|
| Landlords move off WhatsApp/paper for lease tracking | # active landlord accounts with ≥1 property added | 50 landlords |
| Rent collection becomes traceable | % of rent payments made via CMI vs. manual/off-platform | 30% |
| Tenants self-serve instead of contacting landlord directly | # tenant accounts activated per active lease | 60% activation |
| Lease docs generated on-platform | # lease documents generated | 100 documents |

*Targets are placeholders — no market data exists yet. Revisit after first cohort of pilot landlords.*

## 3. User Stories
- As a **Landlord**, I want to add and manage my properties and units, so I can track occupancy and lease status in one place instead of spreadsheets/WhatsApp.
- As a **Landlord**, I want to generate a lease agreement compliant with Moroccan tenancy law, so I don't have to draft contracts manually or hire a notary for every lease.
- As a **Landlord**, I want automated rent reminders sent to tenants, so I don't have to chase payments manually.
- As a **Landlord**, I want to see rent payment status per unit/tenant, so I know who's paid and who's overdue.
- As a **Tenant**, I want a self-service portal to view my lease and payment history, so I don't need to ask the landlord for basic information.
- As a **Tenant**, I want to pay rent online via CMI, so I have a traceable, convenient payment method.
- As a **Tenant**, I want to submit and track maintenance requests, so issues get resolved without informal back-and-forth.
- As a **Property Manager**, I want to manage properties on behalf of multiple landlords, so I can operate as a delegated manager for several owners.
- As an **Admin**, I want to view platform-wide users and activity, so I can support operations and handle disputes/compliance issues.

## 4. Scope
### In Scope (MVP)
- Auth + role-based access (Landlord, Tenant, Property Manager, Admin)
- Landlord dashboard: property, unit, and lease CRUD
- Lease document generation from a Moroccan-law-compliant template (PDF)
- Tenant self-service portal: view lease, view/pay rent, view payment history
- CMI online rent payment integration
- Automated rent due/overdue reminders (email)
- Maintenance request submission and status tracking (Tenant → Landlord)
- Admin: user list, basic platform oversight

### Out of Scope (MVP)
- Bina (construction module) integration — future cross-product link, not now
- Native mobile apps (web-responsive only)
- SMS notifications (email only for MVP)
- Public property listing/marketplace
- Accounting/tax reporting exports
- Multi-language beyond one locale at launch (default French; Arabic/English deferred — **assumption, confirm with user**)

## 5. Requirements
### Functional
- FR-1: Users authenticate with email/password, scoped to one of 4 roles.
- FR-2: Landlord can create/edit/archive properties and units.
- FR-3: Landlord can create a lease linking a tenant to a unit, with start/end date and rent amount.
- FR-4: System generates a downloadable lease PDF from a compliant template on lease creation.
- FR-5: System sends automated email reminders on configurable rent due dates.
- FR-6: Tenant can pay rent for their active lease via CMI; payment status updates the lease record.
- FR-7: Tenant can submit a maintenance request with description + optional photo; Landlord can update its status.
- FR-8: Property Manager can be granted access to manage properties for one or more Landlord accounts.
- FR-9: Admin can list users and deactivate an account.

### Non-Functional
- NFR-1: Performance — page loads < 2s on typical broadband; API p95 < 300ms for CRUD endpoints.
- NFR-2: Security — CMI payment data never touches our servers directly (tokenized/redirect flow); all traffic over HTTPS; passwords hashed (bcrypt/Argon2).
- NFR-3: Accessibility — tenant portal meets WCAG AA for core flows (view lease, pay rent, submit request).
- NFR-4: Availability — single-region deployment is acceptable for MVP (no HA requirement yet).

## 6. Constraints & Assumptions
- CMI requires a merchant account and API credentials — external dependency, must be obtained before payment integration can be tested end-to-end.
- Lease document legal compliance requires review by someone with Moroccan tenancy law knowledge — not something engineering can self-certify. Flagged as a risk below.
- Primary language assumed French for MVP UI; confirm before UI work starts.
- Deployment target for now is local Docker Compose (localhost); Kubernetes is not required for MVP scale and will only be introduced if a real scaling/ops need appears (YAGNI).

## 7. Risks
| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Generated lease documents don't meet legal requirements | M | H | Get template reviewed by a legal resource before enabling in production; mark as "draft, review before signing" until confirmed |
| CMI integration delays (merchant onboarding, sandbox access) | M | M | Build payment flow behind an interface so a stub/mock can unblock other work while CMI access is pending |
| Low digital adoption by landlords used to WhatsApp/paper | M | M | Keep onboarding and core flows minimal; validate with a small pilot group before wider rollout |
| Stack pivot (Next.js → Angular/Spring Boot) discards prior stack decisions | L | L | This PRD and all foundation docs are being rewritten for the new stack in this session |

## 8. Timeline
| Milestone | Target Date |
|---|---|
| PRD Approved | 2026-07-02 |
| Foundation docs (all) approved | 2026-07-02 |
| Scaffolding (Angular + Spring Boot + Docker Compose skeleton) | Sprint 1 |
| First vertical slice (auth + property/lease CRUD) | Sprint 2 |
