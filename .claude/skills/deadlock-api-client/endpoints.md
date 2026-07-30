# Confirmed deadlock-api endpoints

> Last verified: 2026-07-22 against live OpenAPI at `https://api.deadlock-api.com/openapi.json`
> Update this file whenever an endpoint or field changes (Valve patches often).

## Base

- Host: `https://api.deadlock-api.com`
- OpenAPI spec: `https://api.deadlock-api.com/openapi.json`
- Generated clients: `https://github.com/deadlock-api/openapi-clients`
- Auth: No API key required, but providing one doubles the rate limit (see below)
- Rate limits (shared across all analytics endpoints):
  - IP (no key): 200 req/min
  - API key: 400 req/min
  - Global: 2000 req/min

---

## Endpoints we use

### Hero aggregate stats — daily snapshot
- Path: `GET /v1/analytics/hero-stats`
- Key query params:
  - `min_unix_timestamp` / `max_unix_timestamp` (int64) — Unix epoch seconds; defaults to last 30 days
  - `min_average_badge` / `max_average_badge` (int32, 0–116) — rank bucket filter
  - `game_mode` (string) — `normal` | `street_brawl` | `explore_n_y_c` | `internal`; defaults to `normal`
  - `bucket` (string) — `no_bucket` | `avg_badge` | `start_time_day` | `start_time_week` etc.
  - `min_duration_s` / `max_duration_s` (int64, 0–7000s)
  - `min_hero_matches` (int64) — minimum matches played with hero
  - `account_ids` (array[int32]) — filter by specific players, max 1000
- Response: array of `AnalyticsHeroStats` objects
- Confirmed response fields (verified 2026-07-22 against live API):
  - `hero_id` (int), `bucket` (int — rank bucket, 0 = all ranks), `wins` (int), `losses` (int), `matches` (int)
  - No pre-computed `win_rate` or `pick_rate` — derive as `wins/matches` and store raw counts
  - Additional combat fields present (total_kills, total_deaths, etc.) — ignored for now
- Maps to DTO: `HeroStatsDto` → fact table `hero_stats_snapshot`

### Item aggregate stats — daily snapshot
- Path: `GET /v1/analytics/item-stats`
- Key query params:
  - `min_unix_timestamp` / `max_unix_timestamp` (int64) — Unix epoch seconds; defaults to last 30 days
  - `min_average_badge` / `max_average_badge` (int32, 0–116) — rank bucket filter
  - `game_mode` (string) — defaults to `normal`
  - `hero_ids` (string) — comma-separated hero filter
  - `bucket` (string) — `hero` | `team` | `game_time_min` | time-based buckets
  - `min_matches` (int32) — minimum match count for inclusion; defaults to 20
  - `min_bought_at_s` / `max_bought_at_s` (int32) — item purchase time in seconds
  - `account_ids` (array[int32]) — filter by specific players, max 1000
- Response: array of `ItemStats` objects
- Confirmed response fields (verified 2026-07-22 against live API):
  - `item_id` (int), `bucket` (int), `wins` (int), `losses` (int), `matches` (int), `players` (int)
  - `avg_buy_time_s` (double), `avg_sell_time_s` (double), `avg_buy_time_relative` (double), `avg_sell_time_relative` (double)
  - No pre-computed `win_rate` — derive as `wins/matches`
- Maps to DTO: `ItemStatsDto` → fact table `item_stats_snapshot`
- Caching: 6-hour cache upstream; one daily pull is sufficient

### Assets: client versions (patch/build identifiers)
- Path: `GET /v1/assets/client-versions`
- Query params: none
- Response: array of integers (e.g. `[6518, 6519, ...]`), sorted ascending (oldest first)
- No dedicated patch notes endpoint exists — these version integers serve as patch IDs (`build_number`)
- Use the latest value to tag snapshot rows; map to `patch_id` in the patch calendar
- Maps to: patch calendar dimension table `patches`

### Assets: heroes (dimension refresh)
- Path: `GET /v1/assets/heroes`
- Query params: `language` (string, optional), `client_version` (int32, optional)
- Response: array of Hero objects
- Response fields we consume: `hero_id` (integer), `class_name` (string), `display_name` (string)
- Maps to DTO: `HeroAssetDto` → dimension table `heroes`
- Refresh occasionally — data changes only on patches

### Assets: items (dimension refresh)
- Path: `GET /v1/assets/items`
- Query params: `language` (string, optional), `client_version` (int32, optional)
- Response: array of Item objects
- Response fields we consume: `item_id` (integer), `class_name` (string), `display_name` (string)
- Maps to DTO: `ItemAssetDto` → dimension table `items`
- Refresh occasionally — data changes only on patches

---

## Daily dumps (bulk / backfill)

- Location: confirm from live OpenAPI or `https://api.deadlock-api.com` docs
- Format: confirm (likely Parquet or CSV)
- Cadence: daily
- Used by: historical backfill job (Python + pandas/pyarrow), NOT the live ingestion path
- Do NOT loop live endpoints to reconstruct history — download and process the dump

---

## Common query parameter notes

- `game_mode` default is `normal` — pass it explicitly so behaviour is predictable
- Badge scale is 0–116; map rank buckets to `min_average_badge`/`max_average_badge` ranges
- Timestamp params are Unix epoch **seconds** (int64), not milliseconds
- `account_ids` arrays are capped at 1000 entries
- Analytics responses are cached ~6 hours upstream; polling more frequently than that adds no value

---

## Notes / gotchas observed

- No dedicated patch notes endpoint — use `/v1/assets/client-versions` integers as patch IDs
- Hero/item asset endpoints accept `client_version` to retrieve patch-specific metadata
- Field names in analytics responses may drift between Valve patches — revalidate DTO mapping after major updates
- `win_rate` / `pick_rate` may be pre-computed or may need deriving from raw counts; always store raw numerators and denominators regardless
