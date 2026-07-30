---
name: flyway-migrations
description: >-
  Rules for writing Flyway + PostgreSQL migrations in this project: file
  naming, forward-only edits, safe/concurrent index creation, and the snapshot
  fact-table, dimension, and patch-calendar schema conventions. Use this
  whenever creating or editing any .sql migration or changing the database
  schema in any way.
---

# Flyway migrations

How this project evolves its PostgreSQL schema. Migrations are easy to get
subtly wrong and hard to undo once shared, so follow these rules exactly.

## Location and naming

- Versioned migrations: `src/main/resources/db/migration/`
- Versioned files: `V<version>__<snake_case_description>.sql`
  (e.g. `V3__add_hero_stats_snapshot.sql`). Note the **double underscore**.
- Repeatable migrations: `R__<description>.sql` — used ONLY for objects safe to
  recreate on every change, i.e. views and materialized views. Flyway re-runs an
  `R__` file whenever its checksum changes, so it's ideal for the analytics
  views that evolve.
- Version numbers must be monotonically increasing and unique. Use the next
  integer; don't reuse or renumber.

## Cardinal rules

1. **Never edit a migration that has already been applied** anywhere (including
   your own machine after running it). Flyway checksums applied migrations; a
   changed checksum fails validation. To change schema, write a NEW migration.
2. **Forward-only.** We do not use down migrations. Fixing a mistake means a new
   migration that corrects it, not editing history.
3. **One logical change per migration.** A reader should be able to name what a
   migration does in one sentence.
4. **Never run `flyway clean` against anything but a disposable test database.**
   It drops everything. The accumulated snapshot history is irreplaceable.

## PostgreSQL specifics

- Postgres has transactional DDL, and Flyway runs each migration in a single
  transaction by default — good, a failed migration rolls back cleanly.
- **`CREATE INDEX CONCURRENTLY` cannot run inside a transaction.** Put it in its
  own dedicated migration and configure that one migration to run outside a
  transaction via a script config file next to it
  (`V<n>__<name>.sql.conf` containing `executeInTransaction=false`). Confirm the
  exact toggle against the project's Flyway version before relying on it. Use
  `CONCURRENTLY` for indexes added to tables that already hold data so ingestion
  isn't blocked.
- Prefer `TIMESTAMPTZ` over `TIMESTAMP`, `NUMERIC` for rates you'll aggregate,
  and explicit `NOT NULL` + defaults over nullable columns.

## Schema conventions for this project

This is a dimensional (star-ish) model. Keep dimensions and facts separate.

- **Dimension tables** (`heroes`, `items`, `patches`): stable descriptive data,
  natural business key as primary key where sensible (`hero_id`, `item_id`,
  `patch_id`). Refreshed from the API assets endpoints.
- **Patch calendar** (`patches`): `patch_id`, `build_number`, `released_at`
  (`TIMESTAMPTZ`), `notes_url`, `summary`. Everything time-based joins through
  this. `build_number` is how match data maps to a patch.
- **Snapshot fact tables** (`hero_stats_snapshot`, `item_stats_snapshot`): the
  time-series. Every fact row carries `snapshot_date`, `patch_id`, the entity id,
  a `rank_bucket`, the raw counts (`matches`, `wins`, `picks`), and derived
  rates. The counts matter more than the rates — always store the raw numerators
  and denominators so rates can be recomputed and confidence weighted later.
- **Upsert support:** ingestion re-runs must be idempotent, so every fact table
  needs a `UNIQUE` constraint on its natural key
  (e.g. `(snapshot_date, patch_id, hero_id, rank_bucket)`). That constraint is
  what `INSERT ... ON CONFLICT ... DO UPDATE` targets. No unique key = duplicate
  rows on the second run.
- **Indexes:** index the columns the analytics queries filter and group on —
  typically `(hero_id, patch_id, snapshot_date)` and `(patch_id)`. Add them in
  the same migration as the table when it's still empty; use `CONCURRENTLY`
  (see above) when adding to a populated table.
- **Partitioning:** once a fact table grows large, range-partition by
  `snapshot_date`. Introduce this only when volume justifies it, in its own
  migration.
- **Materialized views** (e.g. "biggest movers this patch"): define as `R__`
  repeatable migrations. Document how/when they're refreshed
  (`REFRESH MATERIALIZED VIEW CONCURRENTLY`, which requires a unique index on
  the view).

## Example: a fact table migration

```sql
-- V4__add_hero_stats_snapshot.sql
CREATE TABLE hero_stats_snapshot (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    snapshot_date DATE        NOT NULL,
    patch_id      INT         NOT NULL REFERENCES patches (patch_id),
    hero_id       INT         NOT NULL REFERENCES heroes (hero_id),
    rank_bucket   SMALLINT    NOT NULL,
    matches       INT         NOT NULL,
    wins          INT         NOT NULL,
    picks         INT         NOT NULL,
    win_rate      NUMERIC(6,5) NOT NULL,
    pick_rate     NUMERIC(6,5) NOT NULL,
    CONSTRAINT uq_hero_stats_snapshot
        UNIQUE (snapshot_date, patch_id, hero_id, rank_bucket)
);

CREATE INDEX idx_hero_stats_hero_patch_date
    ON hero_stats_snapshot (hero_id, patch_id, snapshot_date);
```

## Testing

- Run migrations against a disposable PostgreSQL via Testcontainers in
  integration tests — this proves the migration applies cleanly on an empty DB
  and catches ordering/checksum problems before they reach a shared environment.
- Let `flyway validate` run on app startup. Never disable validation to "make it
  work" — a validation failure means a migration was edited or is out of order,
  which must be fixed, not silenced.

## Before finishing, check

- [ ] New file is `V<next>__<snake_case>.sql`, not an edit of an applied one.
- [ ] Fact tables have a `UNIQUE` natural-key constraint for `ON CONFLICT`.
- [ ] Raw counts stored, not just derived rates.
- [ ] Indexes cover the query filters; `CONCURRENTLY` used on populated tables.
- [ ] Views/materialized views are `R__` repeatable migrations.
- [ ] Migration applies cleanly on a fresh Testcontainers Postgres.
