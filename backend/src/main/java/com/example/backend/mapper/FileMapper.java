package com.example.backend.mapper;

import com.example.backend.domain.File;
import com.example.backend.dto.FileDTO;

public class FileMapper {
  
  public static FileDTO toDto(File file) {
    return new FileDTO(file.getId(), file.getFileStatus(), file.getFileType(), file.getMimeType(),
        file.getOriginalFilename(), file.getSizeBytes(), file.getCreatedAt(), file.getExpiresAt());
  }
}
