# Security Baseline: Darkom.ma
**Architecture Reference**: docs/architecture-darkom.md
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: Security Engineer

## 1. Threat Model (5-Minute)
- **What are we building?** A SaaS handling tenancy contracts, personal data of landlords/tenants, and rent payments (via CMI, a licensed gateway — we don't hold card data).
- **Who would attack it?** Opportunistic attackers (credential stuffing, scraping tenant PII), a malicious tenant/landlord trying to manipulate their own lease/payment records, and possibly a competitor scraping property data. Nation-state-level threats are out of scope for a Moroccan rental SaaS at this stage.
- **Worst outcome?** Unauthorized access to another user's lease/PII (privacy breach), or forged payment confirmation marking rent as "paid" when it wasn't (financial fraud).

## 2. STRIDE Analysis (top risks only)
| Threat | Component | Mitigation | Status |
|---|---|---|---|
| Spoofing | Auth (JWT) | Strong password hashing (BCrypt), rate-limited login, short-lived access tokens | TODO |
| Tampering | CMI payment callback | Verify CMI HMAC/signature on every callback; reject unsigned/invalid calls | TODO |
| Repudiation | Payment & lease actions | Audit log (who/when) on lease creation, payment status changes, maintenance status changes | TODO |
| Info Disclosure | Lease/tenant data across tenants | Resource-ownership checks at service layer (not just role checks) on every lease/payment/maintenance query | TODO |
| DoS | Public auth endpoints | Rate limiting on `/auth/login`; no other public endpoints exist | TODO |
| Elevation of Privilege | Role claim in JWT | Roles set server-side only at issuance, never trusted from client input; role changes require re-issuing a token | TODO |

## 3. Authentication Strategy
- **Type**: JWT — short-lived access token (15 min) returned in response body, refresh token (7 days) in an HttpOnly, Secure, SameSite=Strict cookie.
- **MFA**: Not required for MVP — justified by low current risk profile (pilot users, no large-scale financial custody by us since CMI handles the actual money movement). Revisit if platform grows or handles higher-value transactions directly.
- **Password policy**: Minimum 10 characters, checked against a common-password/breach list (e.g., zxcvbn or HaveIBeenPwned range API) at signup.
- **Session management**: Access token in memory only (never localStorage, to limit XSS blast radius); refresh token HttpOnly cookie with 7-day expiry and rotation on use.

## 4. Authorization Model
- **Pattern**: RBAC (4 roles: LANDLORD, TENANT, PROPERTY_MANAGER, ADMIN) + resource-ownership checks.
- **Roles defined**:
  - `LANDLORD` — owns properties/units/leases they created.
  - `TENANT` — sees only their own active lease(s), payments, maintenance requests.
  - `PROPERTY_MANAGER` — same permissions as Landlord, scoped to properties they've been granted access to (via `property_managers` join table).
  - `ADMIN` — platform-wide read access + user deactivation. No direct edit of financial/lease records (avoids a single compromised admin account causing data tampering).
- **Resource-level checks**: Yes, mandatory — every lease/payment/maintenance endpoint must verify the authenticated user is the owning Landlord/PM or the assigned Tenant before returning data, enforced in the service layer (not left to the controller or trusted from client-supplied IDs alone).

## 5. Data Protection
- **PII fields**: Full name, email, phone, national ID (CIN) if collected for lease compliance, address.
- **Encryption at rest**: PostgreSQL disk-level encryption via the host/volume (deferred to deployment environment — not application-level for MVP; revisit if a compliance requirement demands column-level encryption for CIN).
- **Encryption in transit**: HTTPS enforced in any non-local deployment; HSTS enabled once a real domain exists. Localhost dev may run plain HTTP for convenience — never for anything beyond a developer's own machine.
- **Secrets management**: All secrets (JWT signing key, CMI API keys, email provider key, DB credentials) via environment variables, never committed. `.env.example` documents required keys with no real values.

## 6. Security Requirements for Dev Team
- [ ] All inputs validated server-side (Bean Validation / `@Valid` on DTOs — never trust client validation alone)
- [ ] Output encoded for context (Angular's default HTML sanitization must not be bypassed with `[innerHTML]` on untrusted content)
- [ ] No secrets in code, logs, or error messages (error responses must not leak stack traces in production profile)
- [ ] HTTPS only, security headers configured (HSTS, X-Content-Type-Options, X-Frame-Options) once deployed beyond localhost
- [ ] Dependencies scanned in CI (Trivy for Maven + npm deps, see docs/devops-darkom.md)
