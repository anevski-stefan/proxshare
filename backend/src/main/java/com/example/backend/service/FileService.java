package com.example.backend.service;

import com.example.backend.domain.File;
import com.example.backend.repository.FileRepository;
import com.example.backend.web.exceptions.FileNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FileService {

  private final FileRepository fileRepository;

  public FileService(FileRepository fileRepository) {
    this.fileRepository = fileRepository;
  }

  public File getFileById(UUID id) {
    return fileRepository.findById(id)
        .orElseThrow(() -> new FileNotFoundException("File not found"));
  }
}
