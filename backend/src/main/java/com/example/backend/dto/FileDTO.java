package com.example.backend.dto;

import com.example.backend.domain.enums.FileStatus;
import com.example.backend.domain.enums.FileType;
import java.time.Instant;
import java.util.UUID;

public record FileDTO(
    UUID id,
    FileStatus fileStatus,
    FileType fileType,
    String mimeType,
    String originalFilename,
    Long sizeBytes,
    Instant createdAt,
    Instant expiresAt) {}
