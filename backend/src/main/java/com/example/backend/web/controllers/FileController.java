package com.example.backend.web.controllers;

import com.example.backend.dto.FileDTO;
import com.example.backend.mapper.FileMapper;
import com.example.backend.service.FileService;
import com.example.backend.web.exceptions.InvalidFileIdException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {

  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @GetMapping("/{id}")
  public ResponseEntity<FileDTO> getFile(@PathVariable String id) {

    UUID fileId;

    try {
      fileId = UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new InvalidFileIdException();
    }
    return ResponseEntity.ok(FileMapper.toDto(fileService.getFileById(fileId)));
  }

}
