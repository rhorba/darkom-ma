# DevOps Foundation: Darkom.ma
**Architecture**: docs/architecture-darkom.md
**Security**: docs/security-darkom.md
**Version**: 1.0 | **Date**: 2026-07-02 | **Author**: DevOps/DevSecOps

## 1. Environment Strategy
| Environment | Purpose | Deploy Trigger |
|---|---|---|
| local | Development, MVP target | `docker compose up` on localhost, manual |
| staging | Not set up yet | N/A — introduce when a real deploy target exists beyond localhost |
| production | Not set up yet | N/A — introduce when the product is ready for real users beyond a local pilot |

*Only `local` is in scope for this phase, matching the user's explicit "deployment on localhost" request. Staging/production rows are left as placeholders so the table isn't silently incomplete — not built now (YAGNI).*

## 2. CI Pipeline (GitHub Actions)
```yaml
stages:
  - lint            # ESLint (Angular) + Checkstyle/Spotless (Java)
  - test            # JUnit+Testcontainers (backend), Jasmine/Karma (frontend) — fail if coverage < 80% combined
  - security-scan   # SAST (Semgrep), SCA (Trivy on Maven + npm deps), secrets (Gitleaks)
  - build           # Docker images: darkom-frontend, darkom-backend
  - deploy-local    # N/A for CI — local deploy is `docker compose up`, run manually by the developer
```
*No `deploy-staging`/`deploy-prod` stages yet — nothing to deploy to. Added when a real environment exists.*

## 3. Infrastructure
- **Hosting**: Localhost only, via Docker Compose, for this phase.
- **Compute**: 3 containers — `frontend` (Angular build served by Nginx), `backend` (Spring Boot fat jar), `db` (PostgreSQL 16).
- **Database**: Containerized Postgres with a named volume (`darkom_pgdata`) for persistence across restarts.
- **Secrets**: `.env` file (git-ignored) consumed by Docker Compose; `.env.example` documents required keys with placeholder/no values (see Rule 10 — real values collected from user before EXECUTE, never committed).
- **Monitoring**: Spring Boot Actuator `/actuator/health` exposed for basic liveness check. No external monitoring stack (Prometheus/Grafana etc.) — not justified at localhost/pilot scale.

## 4. Security Scanning Gates
| Scanner | Scan Type | Fail Threshold |
|---|---|---|
| Semgrep | SAST — code vulnerabilities | Critical findings |
| Trivy | SCA — dependency CVEs (Maven + npm) | Critical CVEs |
| Gitleaks | Secrets detection | Any secrets found |

## 5. Docker Setup

**Backend** (`backend/Dockerfile`, as-built):
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/data/lease-documents /app/data/maintenance-photos && chown -R 1000:1000 /app/data
EXPOSE 8080
USER 1000:1000
ENTRYPOINT ["java", "-jar", "app.jar"]
```
*Non-root container: the data directories must be created and chowned to the runtime user (1000:1000) before switching `USER`, or the mounted volumes are unwritable - caught via a docker-compose smoke test in Story 2.2, not by any automated test.*

**Frontend** (`frontend/Dockerfile`, as-built):
```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```
*`nginx.conf` adds SPA fallback (`try_files $uri $uri/ /index.html;`) - without it, nginx's default config only serves exact file matches, so any direct/refreshed navigation to a client-side route (e.g. `/properties`, `/my-lease`) 404s instead of loading the Angular shell. Found via the E2E suite, not caught by any earlier manual curl-based smoke test since those never did a hard browser navigation to a deep route.*

**`docker-compose.yml`** (root, as-built - all three services, host port overrides via `.env` since this dev machine has a port collision with an unrelated project on 4200/8080):
```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - darkom_pgdata:/var/lib/postgresql/data
    ports: ["${DB_PORT_HOST:-5432}:5432"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME}"]
      interval: 5s
      timeout: 5s
      retries: 10

  backend:
    build: ./backend
    environment:
      DB_HOST: db
      DB_PORT: 5432
      DB_NAME: ${DB_NAME}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      COOKIE_SECURE: ${COOKIE_SECURE:-true}
      CMI_MERCHANT_ID: ${CMI_MERCHANT_ID}
      CMI_API_KEY: ${CMI_API_KEY}
      CMI_SECRET_KEY: ${CMI_SECRET_KEY}
      CMI_CALLBACK_URL: ${CMI_CALLBACK_URL}
      EMAIL_API_KEY: ${EMAIL_API_KEY}
      APP_BASE_URL: ${APP_BASE_URL}
      REMINDER_DAYS_BEFORE_DUE: ${REMINDER_DAYS_BEFORE_DUE:-3}
      REMINDER_CRON: "${REMINDER_CRON:-0 0 8 * * *}"
    volumes:
      - darkom_lease_documents:/app/data/lease-documents
      - darkom_maintenance_photos:/app/data/maintenance-photos
    depends_on:
      db:
        condition: service_healthy
    ports: ["${BACKEND_PORT_HOST:-8080}:8080"]

  frontend:
    build: ./frontend
    depends_on: [backend]
    ports: ["${FRONTEND_PORT_HOST:-4200}:80"]

volumes:
  darkom_pgdata:
  darkom_lease_documents:
  darkom_maintenance_photos:
```
*Important machine-specific gotcha: `CMI_CALLBACK_URL` and `APP_BASE_URL` must match whatever host ports this specific machine actually maps to (via `.env`'s `*_PORT_HOST` overrides), or the CMI mock redirect URL and CORS `Access-Control-Allow-Origin` end up pointing at the wrong port and requests silently fail from a real browser (backend logic itself is unaffected - confirmed by curl, which doesn't send the `Origin` header a browser does). Only surfaces when testing through an actual browser, not via curl-based smoke tests.*

## 5a. End-to-End Testing (`e2e/`)
Playwright suite (`e2e/tests/critical-flows.spec.ts`), independent of the Claude-in-Chrome extension used during development - drives a real browser against the full docker-compose stack. Covers the top user journeys from docs/ux-darkom.md: Landlord creates property/unit/lease, Tenant views the lease and pays rent through the real mock CMI redirect page, Tenant submits a maintenance request, Landlord updates its status, Admin deactivates a user. Not wired into CI yet (requires the full stack running, unlike the unit/integration tests) - run manually via `cd e2e && npx playwright test` against a running `docker compose up` stack. Records video per run to `.recordings/` (gitignored - binary build artifact).

## 6. Kubernetes (documented, not built)
Per SDR-2 (docs/system-design-darkom.md), Kubernetes is **not** set up for MVP — Docker Compose on localhost fully covers current needs. If a real multi-node or auto-scaling requirement emerges later, the path is: containerize identically (already true), write Deployment/Service manifests per container, add an Ingress, and introduce a managed Postgres instead of the in-cluster one. Not created now — would be speculative infrastructure with no current need (YAGNI).

## 7. Monitoring Baseline
| Signal | Tool | Alert Threshold |
|---|---|---|
| Health | Spring Boot Actuator `/actuator/health` | Manual check for MVP — no alerting pipeline yet |
| Logs | Container stdout (`docker compose logs`) | No log aggregation yet — introduce only if debugging local logs becomes impractical |
