# Deadlock Meta History — CLAUDE.md

## What this project is

A Spring Boot + PostgreSQL web app that tracks Deadlock hero/item win/pick rates
as a time-series across patches, powered by the open deadlock-api.com.
See `PLAN.md` for the full phased build plan.

## Stack

- Java 21, Spring Boot 3.3.x
- PostgreSQL 16 + Flyway (schema migrations)
- Resilience4j (retry, rate limiter for outbound API calls)
- Caffeine (response caching)
- Testcontainers + WireMock (integration tests)
- Docker Compose (local dev)
- React + Recharts (frontend, Phase 4)

## Run locally

```bash
# Start Postgres + app
docker compose up --build

# App only (requires Postgres already running on 5432)
./gradlew bootRun

# Tests (spins up Testcontainers automatically)
./gradlew test
```

Health check: `http://localhost:8080/actuator/health`

## Package layout

```
dim.deadlockrts
├── config/       # Spring beans: RestClient, Resilience4j, Caffeine
├── client/       # deadlock-api DTOs and HTTP client
├── domain/       # JPA entities
├── ingestion/    # @Scheduled jobs
└── api/          # REST controllers (Phase 3+)
```

## Database migrations

All schema changes go through Flyway. See `.claude/skills/flyway-migrations/SKILL.md` for rules.

- Migration files: `src/main/resources/db/migration/`
- Naming: `V<n>__<snake_case_description>.sql` (double underscore)
- Repeatable (views only): `R__<description>.sql`
- Never edit an applied migration — write a new one

## API client

All calls to deadlock-api.com follow `.claude/skills/deadlock-api-client/SKILL.md`.
Confirmed endpoints are in `.claude/skills/deadlock-api-client/endpoints.md`.

- Base URL and User-Agent come from `application.yml` under `deadlock-api.*`
- Every call is wrapped with Resilience4j retry + rate limiter
- DTOs are Java records; Jackson ignores unknown properties globally

## Coding conventions

- No comments unless the WHY is non-obvious
- No unnecessary abstractions — solve the problem at hand
- Integration tests use Testcontainers (real Postgres) — no mocking the database
- Tests are written in the same phase as the feature, never deferred
- One Flyway migration per logical change

## Config

`application.yml` is the single source of truth for config. No hardcoded URLs,
credentials, or contact strings in code. Secrets (API key, DB password) are
overridden via environment variables in Docker Compose / production.
