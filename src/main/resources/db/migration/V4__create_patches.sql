CREATE TABLE patches (
    patch_id     INT              NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    build_number INT              NOT NULL UNIQUE,
    released_at  TIMESTAMPTZ,
    notes_url    VARCHAR(500),
    summary      TEXT
);

CREATE INDEX idx_patches_build_number ON patches (build_number);
