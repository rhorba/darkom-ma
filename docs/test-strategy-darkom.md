# Test Strategy: Darkom.ma
**Architecture Reference**: docs/architecture-darkom.md
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: Test Architect

## 1. Risk Assessment
| Component | Impact | Frequency | Complexity | Test Level |
|---|---|---|---|---|
| Auth (login, JWT, role checks) | H | H | M | Maximum |
| Payment module (CMI init + callback) | H | M | H | Maximum |
| Lease CRUD + PDF generation | H | M | M | High |
| Resource-ownership checks (cross-tenant access) | H | M | M | High |
| Maintenance requests | M | M | L | Standard |
| Property/unit CRUD | M | H | L | Standard |
| Admin user list | L | L | L | Minimal |

## 2. Test Pyramid Targets
| Layer | Coverage Target | Tooling |
|---|---|---|
| Unit (backend) | ≥ 60% of service-layer business logic | JUnit 5 + Mockito |
| Unit (frontend) | ≥ 60% of component/service logic | Jasmine + Karma (Angular CLI default) |
| Integration (backend) | ≥ 40% of controller + repository layer, real Postgres | Testcontainers + Spring Boot Test |
| E2E | Critical happy paths only (login, create lease, pay rent, submit maintenance) | Playwright |
| **Combined gate** | **≥ 80%** (unit + integration) — non-negotiable | CI blocks merge if below (see docs/devops-darkom.md) |

## 3. ATDD Acceptance Scenarios (critical paths)
```gherkin
Feature: Lease creation

  Scenario: Landlord creates a lease for a vacant unit
    Given a Landlord is authenticated and owns a property with a vacant unit
    When they submit a lease with a valid tenant email, start date, end date, and rent
    Then a lease record is created with status ACTIVE
    And a lease PDF is generated and downloadable
    And the unit status changes to OCCUPIED

  Scenario: Landlord attempts to lease an already-occupied unit
    Given a unit already has an ACTIVE lease
    When the Landlord submits a new lease for that unit
    Then the request is rejected with a clear error
    And no second lease record is created

Feature: Rent payment

  Scenario: Tenant pays rent successfully via CMI
    Given a Tenant has a PENDING payment on their active lease
    When they complete payment on CMI and CMI sends a valid signed callback
    Then the payment status becomes PAID
    And the payment is timestamped with paid_at

  Scenario: Forged payment callback is rejected
    Given a payment callback arrives without a valid CMI signature
    When the backend processes it
    Then the payment status is NOT changed
    And the attempt is logged

Feature: Cross-tenant data isolation

  Scenario: Tenant cannot view another tenant's lease
    Given Tenant A and Tenant B each have their own lease
    When Tenant A requests Tenant B's lease by ID
    Then the API returns 403/404, not the lease data
```

## 4. Adversarial Checklist (high-risk components only)
- [ ] Input abuse: empty/oversized lease dates, negative or zero rent amounts, malformed emails on lease creation
- [ ] Auth abuse: expired/tampered JWT accepted?, role claim tampering, login rate-limit bypass attempts
- [ ] CMI callback abuse: replayed callback (same transaction ID twice), missing/invalid signature, callback for a non-existent lease ID
- [ ] Race conditions: two simultaneous lease creations for the same unit (double-booking)
- [ ] Business logic: lease end_date before start_date, payment due_date outside lease term

## 5. Release Gate Criteria
- [ ] All acceptance scenarios above pass
- [ ] Combined unit + integration coverage ≥ 80% (backend and frontend measured separately, both must clear)
- [ ] No critical/high security findings open (Trivy/Semgrep — see DevOps doc)
- [ ] E2E happy path passes (login → create lease → pay rent → submit maintenance)
