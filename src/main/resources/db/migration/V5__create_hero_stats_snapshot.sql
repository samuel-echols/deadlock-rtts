CREATE TABLE hero_stats_snapshot (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    snapshot_date DATE         NOT NULL,
    patch_id      INT          NOT NULL REFERENCES patches (patch_id),
    hero_id       INT          NOT NULL REFERENCES heroes (hero_id),
    rank_bucket   SMALLINT     NOT NULL,
    matches       INT          NOT NULL,
    wins          INT          NOT NULL,
    losses        INT          NOT NULL,
    win_rate      NUMERIC(8,5) NOT NULL,
    CONSTRAINT uq_hero_stats_snapshot
        UNIQUE (snapshot_date, patch_id, hero_id, rank_bucket)
);

CREATE INDEX idx_hero_stats_hero_patch_date
    ON hero_stats_snapshot (hero_id, patch_id, snapshot_date);

CREATE INDEX idx_hero_stats_patch
    ON hero_stats_snapshot (patch_id);
