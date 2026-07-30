CREATE TABLE schema_meta (
    key   VARCHAR(100) NOT NULL PRIMARY KEY,
    value TEXT         NOT NULL
);

INSERT INTO schema_meta (key, value) VALUES ('schema_version', '1');
