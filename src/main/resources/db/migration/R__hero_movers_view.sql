DROP MATERIALIZED VIEW IF EXISTS hero_movers;

CREATE MATERIALIZED VIEW hero_movers AS
WITH ranked AS (
    SELECT
        h.hero_id,
        hr.display_name,
        h.patch_id,
        h.win_rate,
        LAG(h.win_rate) OVER (PARTITION BY h.hero_id ORDER BY h.patch_id) AS prev_win_rate
    FROM hero_stats_snapshot h
    JOIN heroes hr ON hr.hero_id = h.hero_id
    WHERE h.rank_bucket = 0
)
SELECT
    hero_id,
    display_name,
    patch_id,
    win_rate,
    prev_win_rate,
    (win_rate - prev_win_rate) AS win_rate_delta
FROM ranked
WHERE prev_win_rate IS NOT NULL
ORDER BY ABS(win_rate - prev_win_rate) DESC;

CREATE UNIQUE INDEX uq_hero_movers_hero_patch_mv
    ON hero_movers (hero_id, patch_id);
6