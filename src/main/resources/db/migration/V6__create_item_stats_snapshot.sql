CREATE TABLE item_stats_snapshot (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    snapshot_date   DATE         NOT NULL,
    patch_id        INT          NOT NULL REFERENCES patches (patch_id),
    item_id         INT          NOT NULL REFERENCES items (item_id),
    rank_bucket     SMALLINT     NOT NULL,
    matches         INT          NOT NULL,
    wins            INT          NOT NULL,
    losses          INT          NOT NULL,
    players         INT          NOT NULL,
    win_rate        NUMERIC(8,5) NOT NULL,
    avg_buy_time_s  NUMERIC(10,3),
    CONSTRAINT uq_item_stats_snapshot
        UNIQUE (snapshot_date, patch_id, item_id, rank_bucket)
);

CREATE INDEX idx_item_stats_item_patch_date
    ON item_stats_snapshot (item_id, patch_id, snapshot_date);

CREATE INDEX idx_item_stats_patch
    ON item_stats_snapshot (patch_id);
