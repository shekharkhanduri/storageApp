CREATE TABLE files (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename     VARCHAR(255) NOT NULL,
    object_key   VARCHAR(500) NOT NULL,
    size         BIGINT NOT NULL,
    content_type VARCHAR(100),
    extension    VARCHAR(20),
    file_type    VARCHAR(20) NOT NULL,
    status       VARCHAR(20) DEFAULT 'PENDING',
    shared_with  TEXT[],
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_files_owner ON files(owner_id);
CREATE INDEX idx_files_name_fts ON files USING GIN(to_tsvector('english', filename));
