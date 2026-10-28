-- hero_movers used to be a materialized view; DROP VIEW errors on a matview and
-- DROP MATERIALIZED VIEW errors on a plain view, so check which one exists.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_matviews WHERE matviewname = 'hero_movers') THEN
        DROP MATERIALIZED VIEW hero_movers;
    END IF;
END $$;

DROP VIEW IF EXISTS hero_movers;
DROP VIEW IF EXISTS hero_patch_stats;

-- Daily snapshots cover disjoint days, so pooling raw counts gives the per-patch rate.
CREATE VIEW hero_patch_stats AS
SELECT
    s.hero_id,
    s.patch_id,
    p.build_number,
    SUM(s.matches) AS matches,
    SUM(s.wins)    AS wins,
    SUM(s.losses)  AS losses,
    ROUND(SUM(s.wins)::NUMERIC / SUM(s.matches), 5) AS win_rate
FROM hero_stats_snapshot s
JOIN patches p ON p.patch_id = s.patch_id
WHERE s.rank_bucket = 0
GROUP BY s.hero_id, s.patch_id, p.build_number;

CREATE VIEW hero_movers AS
WITH patch_order AS (
    SELECT patch_id, LAG(patch_id) OVER (ORDER BY build_number) AS prev_patch_id
    FROM patches
)
SELECT
    cur.hero_id,
    h.display_name,
    cur.patch_id,
    cur.build_number,
    cur.win_rate,
    prev.win_rate                  AS prev_win_rate,
    (cur.win_rate - prev.win_rate) AS win_rate_delta
FROM hero_patch_stats cur
JOIN heroes h       ON h.hero_id = cur.hero_id
JOIN patch_order po ON po.patch_id = cur.patch_id
LEFT JOIN hero_patch_stats prev
       ON prev.hero_id = cur.hero_id
      AND prev.patch_id = po.prev_patch_id;
