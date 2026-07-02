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

**Backend** (`backend/Dockerfile`):
```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
USER 1000:1000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Frontend** (`frontend/Dockerfile`):
```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

FROM nginx:alpine
COPY --from=build /app/dist/darkom-frontend/browser /usr/share/nginx/html
EXPOSE 80
```

**`docker-compose.yml`** (root, MVP shape):
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
    ports: ["5432:5432"]

  backend:
    build: ./backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${DB_NAME}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      CMI_MERCHANT_ID: ${CMI_MERCHANT_ID}
      CMI_API_KEY: ${CMI_API_KEY}
      CMI_SECRET_KEY: ${CMI_SECRET_KEY}
      EMAIL_API_KEY: ${EMAIL_API_KEY}
    depends_on: [db]
    ports: ["8080:8080"]

  frontend:
    build: ./frontend
    depends_on: [backend]
    ports: ["4200:80"]

volumes:
  darkom_pgdata:
```

## 6. Kubernetes (documented, not built)
Per SDR-2 (docs/system-design-darkom.md), Kubernetes is **not** set up for MVP — Docker Compose on localhost fully covers current needs. If a real multi-node or auto-scaling requirement emerges later, the path is: containerize identically (already true), write Deployment/Service manifests per container, add an Ingress, and introduce a managed Postgres instead of the in-cluster one. Not created now — would be speculative infrastructure with no current need (YAGNI).

## 7. Monitoring Baseline
| Signal | Tool | Alert Threshold |
|---|---|---|
| Health | Spring Boot Actuator `/actuator/health` | Manual check for MVP — no alerting pipeline yet |
| Logs | Container stdout (`docker compose logs`) | No log aggregation yet — introduce only if debugging local logs becomes impractical |
