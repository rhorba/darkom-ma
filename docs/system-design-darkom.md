# System Design: Darkom.ma
**PRD Reference**: docs/prd-darkom.md
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: System Designer

## 1. Non-Functional Requirements
| Attribute | Target | Notes |
|---|---|---|
| Availability | Best-effort (local Docker Compose, no SLA) | MVP runs on localhost; revisit if/when deployed to real infra |
| Latency (p99) | < 800ms API | Single-region, single-node MVP; no CDN needed yet |
| Throughput | < 50 RPS | Pilot scale (tens of landlords), monolith easily covers this |
| Data Volume | < 1 GB/month | Text + PDFs + payment metadata only, no media-heavy workloads |
| Retention | Indefinite for lease/payment records (compliance); 1 year for logs | Legal/financial records shouldn't be purged automatically |
| Recovery (RTO) | Not defined for MVP | No production SLA yet — local dev/demo environment |
| Recovery (RPO) | Daily `pg_dump` backup is sufficient for MVP | Automate before any real user data is stored |

## 2. Component Topology
```
[Browser: Angular SPA]
        │ HTTPS (local: HTTP on localhost)
        ▼
[Spring Boot API] (Java 21, single deployable — modular monolith)
  ├── Auth module (Spring Security + JWT)
  ├── Property/Lease module
  ├── Payment module ──── HTTPS ───► [CMI Gateway] (external, hosted payment page/redirect)
  ├── Document module ── generates lease PDFs (server-side, e.g. OpenPDF/iText)
  ├── Notification module ── SMTP/email API ───► [Email Provider] (external)
  └── Maintenance module
        │
        ▼
  [PostgreSQL 16]

All three (Angular build served via Nginx, Spring Boot API, PostgreSQL) run as
containers via Docker Compose on localhost for MVP. No load balancer, no
gateway, no cache — none of that is justified at this scale (YAGNI).
```

## 3. Integration Patterns
| Integration | Pattern | Reason |
|---|---|---|
| CMI (payments) | Redirect / hosted-page + server-to-server webhook/callback | Keeps card data off our servers entirely — CMI handles PCI scope |
| Email provider | REST API (transactional email service) or SMTP | Simple, no queue needed at this volume |
| Angular ↔ Spring Boot | REST/JSON over HTTPS, JWT bearer auth | Standard SPA-to-API pattern, no need for GraphQL/gRPC at this scale |

## 4. Scalability Strategy
- Scaling approach: **vertical only** for MVP — a single Spring Boot instance and single Postgres instance is more than sufficient for pilot scale. Horizontal scaling/load balancing is explicitly deferred (YAGNI) until real usage data justifies it.
- Cache strategy: none for MVP. Revisit only if a specific endpoint shows measured latency problems.
- Queue strategy: none for MVP. Email reminders can run as a scheduled job (Spring `@Scheduled`) inside the monolith; a message queue is not justified at this volume.

## 5. System Design Decision Records
### SDR-1: Modular monolith over microservices
- **NFR Driver**: Throughput/scale targets are trivial (< 50 RPS, pilot user base); team is effectively solo/small.
- **Decision**: Single Spring Boot deployable with clear internal module boundaries (auth, property, payment, document, notification, maintenance), not separate services.
- **Alternatives**: Microservices — rejected, adds deployment/ops complexity with zero benefit at this scale.
- **Re-evaluate when**: A specific module needs independent scaling/deploy cadence, or team grows enough to need independent ownership boundaries.

### SDR-2: Docker Compose over Kubernetes for MVP
- **NFR Driver**: Availability target is "best-effort, localhost"; no HA/multi-node requirement exists yet.
- **Decision**: Ship a `docker-compose.yml` running Angular (Nginx), Spring Boot, and Postgres on localhost. Kubernetes manifests are not created for MVP.
- **Alternatives**: Kubernetes (k3s/kind locally, or a managed cluster) — rejected for now per YAGNI; user explicitly said "if needed" for K8s, and nothing in current scope needs it.
- **Re-evaluate when**: Multi-node deployment, auto-scaling, or a real production rollout beyond localhost is actually needed. DevOps doc will note this as a documented future path, not build it now.

### SDR-3: Synchronous REST, no message broker
- **NFR Driver**: Data volume and throughput targets are both low; reminders are a daily batch, not real-time.
- **Decision**: Spring `@Scheduled` job for rent reminders; direct REST calls for CMI and email integrations.
- **Alternatives**: RabbitMQ/Kafka — rejected, no use case justifies the operational overhead yet.
- **Re-evaluate when**: Reminder volume or integration retry/failure handling needs become complex enough that a queue's durability guarantees are actually needed.
