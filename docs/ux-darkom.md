# UX Foundation: Darkom.ma
**PRD Reference**: docs/prd-darkom.md
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: UX Designer

## 1. User Personas (minimal — YAGNI)
| Persona | Role | Goal | Pain Point |
|---|---|---|---|
| Rachid | Landlord, owns 3-8 units | Track who's paid, generate leases without a notary trip | Juggles WhatsApp threads per tenant, no single view of occupancy/rent status |
| Sara | Tenant | Pay rent easily, know her lease terms, report a broken appliance | No visibility into lease terms after signing; pays via bank transfer with no confirmation |
| Youssef | Property Manager | Manage several owners' properties from one place | Currently uses separate spreadsheets per landlord client |
| Admin (internal) | Platform Admin | Support users, spot abuse | N/A — internal role, minimal UX investment |

## 2. Information Architecture / Site Map
```
[App Root] (auth-gated)
├── /login
├── Landlord/PM area
│   ├── /properties (list + create)
│   │   └── /properties/:id (units list, add unit)
│   ├── /leases (list + create)
│   │   └── /leases/:id (detail, download PDF, payment history)
│   └── /maintenance (requests inbox)
├── Tenant area
│   ├── /my-lease (detail + PDF)
│   ├── /my-lease/pay (CMI redirect flow)
│   └── /my-maintenance (submit + track own requests)
└── Admin area
    └── /admin/users (list, deactivate)
```

## 3. Core User Flows (top 3 journeys)

### Flow 1: Landlord creates a lease
```
[Properties list] → [Select property → unit] → [Create lease form: tenant email, dates, rent]
  → [Submit] → [Lease PDF generated] → [Lease detail page, PDF downloadable]
                        ↓ tenant email invalid/unit already leased
                  [Inline validation error] → [Correct and resubmit]
```

### Flow 2: Tenant pays rent
```
[My Lease] → [Pay button on current due payment] → [Redirect to CMI hosted page]
  → [Tenant completes payment on CMI] → [Redirect back to Darkom] → [Payment status = PAID]
                        ↓ payment fails/cancelled on CMI
                  [Return to Darkom with "payment failed" state] → [Retry button]
```

### Flow 3: Tenant submits maintenance request
```
[My Maintenance] → [New request: description + optional photo] → [Submit]
  → [Request appears as OPEN] → [Landlord updates status] → [Tenant sees IN_PROGRESS/RESOLVED]
                        ↓ empty description
                  [Inline validation, cannot submit]
```

## 4. Key Screen Wireframes (text-based)

### Screen: Landlord — Properties list
```
┌─────────────────────────────────────┐
│ Darkom.ma          [Rachid ▾] [FR]   │
├─────────────────────────────────────┤
│ Mes propriétés          [+ Ajouter]  │
│                                       │
│  ┌─────────────┐  ┌─────────────┐    │
│  │ Résidence A │  │ Villa B     │    │
│  │ 4 unités    │  │ 1 unité     │    │
│  │ 3 occupées  │  │ vacante     │    │
│  └─────────────┘  └─────────────┘    │
├─────────────────────────────────────┤
│ © Darkom.ma                          │
└─────────────────────────────────────┘
```

### Screen: Tenant — My Lease
```
┌─────────────────────────────────────┐
│ Darkom.ma          [Sara ▾] [FR]     │
├─────────────────────────────────────┤
│ Mon bail — Unité 2B                  │
│ Loyer: 3 500 MAD / mois              │
│ Statut prochain paiement: EN ATTENTE │
│                                       │
│      [ Payer maintenant ]            │
│      [ Télécharger le contrat PDF ]  │
├─────────────────────────────────────┤
└─────────────────────────────────────┘
```

## 5. Screen States
| Screen | Empty State | Loading | Error | Success |
|---|---|---|---|---|
| Properties list | "Aucune propriété — Ajoutez-en une" + CTA | Skeleton cards | "Impossible de charger vos propriétés" + retry | New property appears in list |
| My Lease | "Aucun bail actif" (rare — tenant shouldn't reach this if invited correctly) | Skeleton | "Impossible de charger votre bail" | Payment/PDF actions available |
| Maintenance list | "Aucune demande" + CTA to create one | Skeleton rows | Inline error banner | New request appears as OPEN |
| Payment flow | N/A | Spinner during CMI redirect | "Paiement échoué" + retry button | "Paiement confirmé" confirmation state |
