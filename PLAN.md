# Deadlock Meta History — build plan

A web app that tracks how Deadlock's meta changes **over time**: hero and item
win/pick rates as a time-series across patches, annotated with patch notes, plus
a "what this patch actually changed" impact report. Differentiator vs existing
trackers (Mobalytics, statlocker, deadlock-api) is the historical dimension —
they show *current* meta; we accumulate and serve *change over time*.

- **Data source:** `deadlock-api.com` (open-source, MIT, open data). We consume
  it; we do not rebuild it. See the `deadlock-api-client` skill.
- **Stack:** Java 21, Spring Boot 3.x, PostgreSQL + Flyway, Docker, Resilience4j,
  Testcontainers. Light Python (pandas/pyarrow) for dump backfill + patch-notes
  scraping. Frontend: React + Recharts (Thymeleaf + Chart.js is an acceptable
  fallback if JS becomes a blocker).

---

## Kickoff prompt (paste into Claude Code at the repo root)

> I'm building "Deadlock Meta History," a Spring Boot + PostgreSQL web app that
> tracks Deadlock hero/item win & pick rates as a time-series across patches,
> built on top of the open deadlock-api.com. The full plan and acceptance
> criteria are in `PLAN.md`; read it first, then read the skills in
> `.claude/skills/` (`deadlock-api-client` and `flyway-migrations`) and follow
> them.
>
> Start with **Phase 0** only: scaffold the project and get a healthy skeleton
> running. Before writing code, propose the project layout (build tool,
> modules, dependencies) and a draft `CLAUDE.md`, and wait for my okay. Then
> implement Phase 0 and stop at its acceptance criteria so I can verify.
>
> Working norms for the whole project:
> - Work one phase at a time. At the end of each phase, run the tests and pause
>   for me to review before starting the next.
> - Write integration tests with Testcontainers as you go — don't defer testing.
> - Never invent deadlock-api endpoint paths. Use the `deadlock-api-client`
>   skill; when we reach the API work, fetch the live OpenAPI and fill in
>   `.claude/skills/deadlock-api-client/endpoints.md`.
> - Keep `CLAUDE.md` updated as decisions get made.
> - Make small, focused commits with clear messages.

---

## Working agreements

- One phase at a time; stop at each phase's acceptance criteria for review.
- Tests are written within the phase that adds the behavior, not "later."
- The `flyway-migrations` skill governs every schema change; the
  `deadlock-api-client` skill governs every outbound API call.
- The single most important sequencing rule: **stand up daily ingestion early
  (Phase 2) and deploy it, even headless.** Snapshot history only accrues from
  the day the job runs — miss weeks now and they can't be recovered.

---

## Phase 0 — Skeleton & foundation

Goal: a running Spring Boot app wired to Postgres, in Docker, with the project
memory file in place.

- Spring Boot 3.x (Java 21). Gradle (or Maven if preferred).
- Dependencies: web (RestClient), spring-data-jdbc or -jpa, postgresql, flyway,
  resilience4j-spring-boot3, actuator, caffeine; test: testcontainers, wiremock.
- `docker-compose.yml`: app + postgres. (Redis/Caddy added in later phases.)
- `application.yml` with a `deadlock-api.*` config block (base-url, user-agent,
  key placeholder) — no literals in code.
- `CLAUDE.md` at repo root: stack, run/test commands, package layout, coding
  conventions, where the skills live.
- One trivial Flyway migration to prove the pipeline (e.g. a `schema_meta` table).

**Acceptance:** `docker compose up` boots the app; `/actuator/health` is UP;
Flyway applies the initial migration cleanly against Postgres.

---

## Phase 1 — Data contract & dimensions

Goal: confirmed API contract and populated dimension tables.

- Fetch the live OpenAPI from `api.deadlock-api.com`; fill in `endpoints.md`.
- `RestClient` bean + Resilience4j retry/rate-limiter config (per skill).
- Flyway migrations for dimension tables: `heroes`, `items`, `patches`.
- Assets client + a command/job that populates `heroes` and `items` from the API.

**Acceptance:** heroes and items are populated from the live API; `endpoints.md`
reflects the real spec; an integration test (Testcontainers Postgres + WireMock
for the API) covers the assets ingest.

---

## Phase 2 — Daily snapshot ingestion (the core asset)

Goal: idempotent daily job accumulating the time-series. Deploy this early.

- Flyway fact tables: `hero_stats_snapshot`, `item_stats_snapshot` with a UNIQUE
  natural key (e.g. `(snapshot_date, patch_id, hero_id, rank_bucket)`) and raw
  counts stored, not just rates (per `flyway-migrations` skill).
- `@Scheduled` ingestion job: fetch aggregate stats, tag each row with the
  current `patch_id`, upsert via `INSERT ... ON CONFLICT DO UPDATE`.
- Patch calendar: Python scraper for official patch notes → `patches` rows;
  map match `build_number` → `patch_id`.
- Idempotency + checkpoint logging; job survives a mid-run API failure.

**Acceptance:** the job runs on schedule and is safely re-runnable (no duplicate
rows on a second run), verified by an integration test; snapshots visibly
accumulate day over day.

---

## Phase 3 — Analytics API

Goal: serve the historical views.

- Endpoints: hero trend (time-series + patch markers), item trend, patch diff
  (per-entity win-rate delta vs previous patch), movers (biggest risers/fallers).
- Window functions (`LAG`) for patch-over-patch deltas; `R__` repeatable
  migrations for the movers materialized view.
- Caffeine caching keyed to the daily data cadence.

**Acceptance:** endpoints return correct series and deltas against seeded data;
materialized-view refresh works; covered by tests.

---

## Phase 4 — Frontend

Goal: make the data visible and shareable.

- React + Recharts SPA: hero win-rate-over-time chart with patch-line
  annotations; a patch-impact report page; a movers board.
- (Fallback: Thymeleaf + Chart.js if the SPA becomes a blocker.)

**Acceptance:** charts render live data from the API with patches annotated on
the timeline; the patch-impact page reads clearly enough to screenshot and share.

---

## Phase 5 — Deploy & polish

Goal: a public, self-sustaining service.

- Multi-stage Dockerfile; compose with a Caddy reverse proxy for TLS.
- Deploy to a cheap VPS (Hetzner/DigitalOcean) or Fly.io/Railway.
- GitHub Actions CI: build + test on push (deploy step optional).
- Scheduled Postgres backups (the accumulated history is irreplaceable).
- README with visible attribution to deadlock-api.com.

**Acceptance:** public URL is live; CI is green; ingestion runs on schedule in
prod; backups are configured and verified once.
