CREATE TYPE file_status as ENUM('UPLOADING', 'PROCESSING', 'READY', 'EXPIRED', 'ERROR');
CREATE TYPE file_type as ENUM('VIDEO', 'IMAGE', 'OTHER');

CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_filename VARCHAR(512) NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    file_status file_status NOT NULL,
    file_type file_type NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    hls_path VARCHAR(1024) DEFAULT NULL,
    thumbnail_path VARCHAR(1024) DEFAULT NULL,
    uploader_ip VARCHAR(45) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_files_expires_at ON files (expires_at);
CREATE INDEX idx_files_status ON files (file_status);