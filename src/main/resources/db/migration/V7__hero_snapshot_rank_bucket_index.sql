CREATE UNIQUE INDEX uq_hero_snapshot_hero_patch_rank0
    ON hero_stats_snapshot (hero_id, patch_id)
    WHERE rank_bucket = 0;
