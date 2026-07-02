# UI Foundation: Darkom.ma
**UX Reference**: docs/ux-darkom.md
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: UI Designer

## 1. Design Approach
- **Strategy**: Angular Material as the component framework, with a light custom theme layer (color/typography tokens) on top.
- **Rationale**: YAGNI — no brand system exists yet, and building custom components for forms/tables/dialogs would be pure waste for an MVP. Angular Material gives accessible, tested components (forms, tables, dialogs, snackbars) out of the box and pairs natively with Angular.

## 2. Design Tokens (theme layer over Angular Material)
```scss
// Colors
--color-primary:     #0F6E5C;  // deep teal — trust, finance-adjacent product
--color-secondary:   #C98A2C;  // warm ochre accent
--color-background:  #FAFAF8;
--color-surface:     #FFFFFF;
--color-error:       #B3261E;
--color-text:        #1B1B1B;
--color-text-muted:  #5F5F5F;

// Typography
--font-family:   'Inter', 'Helvetica Neue', Arial, sans-serif;
--font-size-sm:  0.875rem;
--font-size-md:  1rem;
--font-size-lg:  1.25rem;
--font-size-xl:  1.75rem;

// Spacing scale
--spacing-xs: 4px;  --spacing-sm: 8px;
--spacing-md: 16px; --spacing-lg: 24px;
--spacing-xl: 40px;
```
*Palette/tokens are a reasonable placeholder default, not a validated brand identity — swap freely once real branding exists. Not a blocker for MVP build.*

## 3. Component Inventory
| Component | Reuse Existing | Build New | Notes |
|---|---|---|---|
| Button | Angular Material `mat-button`/`mat-raised-button` | No | primary/secondary via theme palette |
| Data table (properties/units/leases lists) | `mat-table` | No | sortable, paginated for future scale |
| Form fields | `mat-form-field` + Reactive Forms | No | server-side validation errors surfaced inline |
| Dialog (create property/unit/lease) | `mat-dialog` | No | |
| Status badge (lease/payment/maintenance status) | — | Yes | Small custom component — Material has no direct equivalent, trivial to build |
| Snackbar (success/error toasts) | `mat-snack-bar` | No | |
| Nav shell (top bar + role-based menu) | `mat-toolbar` + `mat-sidenav` | No | |

## 4. Responsive Breakpoints
| Breakpoint | Width | Layout Notes |
|---|---|---|
| Mobile | < 768px | Single column, sidenav collapses to a hamburger menu — tenants are likely mobile-first |
| Tablet | 768–1024px | Two-column lists where applicable |
| Desktop | > 1024px | Landlord dashboard uses full-width tables |

## 5. Accessibility Baseline
- Color contrast: AA minimum (4.5:1 body text, 3:1 large text) — verify custom tokens above against this before implementing.
- Focus indicators: Angular Material's default focus styles retained, not overridden.
- Semantic HTML first; ARIA only where Material doesn't already provide it (it mostly does).
- Language: `lang="fr"` on `<html>` for MVP (French-only per PRD decision).
