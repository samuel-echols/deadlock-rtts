---
name: deadlock-api-client
description: >-
  Conventions for calling the deadlock-api.com REST API from Spring Boot:
  base URL, rate-limit backoff, retries, DTO mapping, choosing dumps vs
  endpoints, and attribution. Use this whenever writing or editing any code
  that fetches Deadlock match, hero, item, patch, or analytics data, even if
  the request doesn't name the API explicitly.
---

# deadlock-api client

How this project talks to `deadlock-api.com` — the open-source (MIT), open-data
community API for Deadlock game data. We are a consumer of this API, not a
rebuilder of it. Treat it as an upstream we don't control.

## Before writing any client code

1. Confirm the live contract. Fetch the OpenAPI/Swagger spec from the API host
   (`https://api.deadlock-api.com`) and check the exact path, query params, and
   response shape. Do NOT trust endpoint paths from memory or from the reference
   file below without confirming — Valve patches often and the schema drifts.
2. Confirm the current rate limits and whether a key/`User-Agent` is required.
   If limits are unclear, assume they are strict and design conservatively.
3. Record what you confirmed in `references/endpoints.md` so the next session
   doesn't have to rediscover it.

## Client setup

- Use Spring's `RestClient` (synchronous is correct here — ingestion is
  scheduled/batch, not reactive). One configured bean, base URL from config.
- Never hardcode the base URL or contact string. Put them in `application.yml`
  under `deadlock-api.*` and inject with `@Value` or `@ConfigurationProperties`.
- Always send a descriptive `User-Agent` with a contact address. Being
  identifiable is part of being a good citizen on a community API.
- Set explicit connect/read timeouts. A hung upstream call must not stall the
  whole ingestion run.

```java
@Bean
RestClient deadlockApiClient(RestClient.Builder builder,
                             @Value("${deadlock-api.base-url}") String baseUrl,
                             @Value("${deadlock-api.user-agent}") String userAgent) {
    return builder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
            .build();
}
```

## Resilience (required, not optional)

Wrap every outbound call with Resilience4j. An external dependency you don't
own will fail intermittently; the ingestion job must survive that.

- Retry with exponential backoff **and jitter**. Never retry in a tight loop.
- Honor `429 Too Many Requests` and any `Retry-After` header — back off for at
  least that long. Do not treat 429 as a normal error to retry immediately.
- Use a `RateLimiter` to cap our own request rate below the documented ceiling.
- Do NOT retry on `4xx` other than `429` — those are our bugs, not transient.

```yaml
resilience4j:
  retry:
    instances:
      deadlockApi:
        max-attempts: 4
        wait-duration: 2s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        enable-randomized-wait: true
  ratelimiter:
    instances:
      deadlockApi:
        limit-for-period: 5
        limit-refresh-period: 1s
        timeout-duration: 10s
```

## Choosing the right data source

Match the source to the job — this is the most important call to get right:

- **Daily snapshots** (the core ingestion job): hit the aggregate analytics
  endpoints (hero/item stats, filterable by patch, rank, date), tag each row
  with the current `patch_id`, and upsert one dated snapshot. Low volume.
- **Historical backfill / heavy analysis**: use the daily database dumps, not
  the live endpoints. Do NOT loop thousands of endpoint calls to reconstruct
  history — download the dump and process it (this is the place for the light
  Python + pandas/pyarrow path). Hammering endpoints for bulk data is abusive.
- **Dimension refresh** (hero/item names, metadata): the assets endpoints,
  refreshed occasionally — this data changes only on patches.
- **Live match events (SSE)**: not needed for historical analytics. Skip it for
  the MVP; revisit only if a real-time feature is added later.

## DTO mapping

- Map responses to Java `record` DTOs, one per endpoint response. Keep DTOs
  separate from JPA entities — never bind an external response straight onto a
  persisted entity.
- Configure Jackson to **ignore unknown properties** globally
  (`FAIL_ON_UNKNOWN_PROPERTIES=false`). The upstream will add fields; that must
  not break ingestion.
- Use `@JsonProperty` for any field whose JSON name differs from your Java name.
- Assume fields can be missing or null across patches. Validate and default at
  the mapping boundary; never let a null from upstream propagate into a snapshot
  row silently.

## Attribution and etiquette

- Credit deadlock-api.com visibly in the app UI and README. It's open data
  provided by volunteers; attribution is the minimum courtesy and a maturity
  signal in a portfolio project.
- Cache aggressively. Their aggregates change at most daily, so a response
  cached for hours is fine and cuts load on both sides.
- Prefer one well-timed daily pull over frequent polling.

## Reference

See `references/endpoints.md` for the confirmed endpoint list. That file is the
source of truth for paths and params **once you've verified them against the
live OpenAPI** — keep it updated, and never invent paths that aren't in it.

## Before finishing, check

- [ ] Base URL, User-Agent, and any key come from config, not literals.
- [ ] Every call is wrapped with retry + rate limiter; 429/Retry-After honored.
- [ ] DTOs are records, ignore unknown fields, tolerate nulls.
- [ ] The job uses dumps for bulk/backfill and endpoints only for daily deltas.
- [ ] `references/endpoints.md` reflects what the live OpenAPI actually returns.
