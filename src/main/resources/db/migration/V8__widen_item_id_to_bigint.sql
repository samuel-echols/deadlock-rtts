-- Upstream item ids exceed INT range (e.g. 4204808176).
ALTER TABLE item_stats_snapshot ALTER COLUMN item_id TYPE BIGINT;
ALTER TABLE items ALTER COLUMN item_id TYPE BIGINT;
