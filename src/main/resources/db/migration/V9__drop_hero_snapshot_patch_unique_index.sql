-- Allowed only one snapshot per hero per patch, which blocks the second daily
-- snapshot within a patch. uq_hero_stats_snapshot already enforces the natural key.
DROP INDEX IF EXISTS uq_hero_snapshot_hero_patch_rank0;
