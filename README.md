# Deadlock Meta History

A web app that tracks how Deadlock's meta evolves **over time** — hero and item win/pick rates as a time-series across patches, annotated with patch notes and a "what this patch actually changed" impact report.

**Differentiator:** existing trackers show the *current* meta. This app accumulates and serves the *change over time*.

> Data provided by [deadlock-api.com](https://deadlock-api.com), an open-source community API (MIT license). Not affiliated with Valve.

---

## Features

- **Hero Trend** — win-rate over time chart with patch boundary annotations
- **Movers Board** — the biggest hero win-rate risers and fallers this patch
- **Patch Diff** — per-hero win-rate delta versus the previous patch, for any given patch

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3, PostgreSQL 16, Flyway |
| Resilience | Resilience4j (retry + rate limiter), Caffeine cache |
| Frontend | React 19, Vite, Recharts, React Router |
| Infra | AWS EC2 + RDS, Docker, GitHub Actions CI/CD |
| Tests | JUnit 5, Testcontainers, WireMock |

## Local development

### Prerequisites
- Java 21
- Node 22
- Docker Desktop

### Run locally

```bash
# Start Postgres
docker compose up postgres -d

# Start Spring Boot (builds React frontend automatically)
./gradlew bootRun

# In a second terminal — Vite dev server with API proxy
cd frontend && npm run dev
```

Then open `http://localhost:5173`.

### Run tests

```bash
./gradlew test -x buildFrontend
```

## Architecture

```
Browser → React SPA (Vite / served by Spring Boot)
                ↓
        Spring Boot :8080
         ├── /api/**         → AnalyticsController (Caffeine cached)
         ├── /actuator/**    → Health / metrics
         └── /**             → SPA fallback (index.html)
                ↓
        PostgreSQL (RDS in prod, Docker locally)
                ↑
        IngestionJob (daily 04:00 UTC)
                ↑
        deadlock-api.com (Resilience4j retry + rate limiter)
```

## Deployment

See the [AWS Setup Guide](docs/aws-setup.md) for full provisioning steps.

CI/CD runs through GitHub Actions (`.github/workflows/ci.yml`):
- Every push: build + test
- Push to `main`: build the Docker image, push to ECR, then deploy to EC2 over SSH

## Attribution

This project consumes data from **[deadlock-api.com](https://deadlock-api.com)**, an open-source (MIT-licensed) community API for Deadlock game data maintained by volunteers. Please consider contributing to or supporting that project.
