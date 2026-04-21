package com.example.backend.web.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.domain.File;
import com.example.backend.domain.enums.FileStatus;
import com.example.backend.domain.enums.FileType;
import com.example.backend.service.FileService;
import com.example.backend.web.exceptions.FileNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileController.class)
class FileControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  FileService fileService;

  @Test
  void getFile_ok() throws Exception {
    var fileId = UUID.randomUUID();
    when(fileService.getFileById(fileId)).thenReturn(buildFile(fileId));

    mockMvc.perform(get("/api/files/{id}", fileId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(fileId.toString()))
        .andExpect(jsonPath("$.fileStatus").value("UPLOADING"))
        .andExpect(jsonPath("$.fileType").value("OTHER"))
        .andExpect(jsonPath("$.mimeType").value("text/plain"))
        .andExpect(jsonPath("$.originalFilename").value("test.txt"))
        .andExpect(jsonPath("$.sizeBytes").value(1024));
  }

  @Test
  void getFile_notFound() throws Exception {
    var fileId = UUID.randomUUID();
    when(fileService.getFileById(fileId)).thenThrow(new FileNotFoundException("not found"));

    mockMvc.perform(get("/api/files/{id}", fileId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  void getFile_invalidId() throws Exception {
    mockMvc.perform(get("/api/files/not-a-valid-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
  }

  private File buildFile(UUID id) {
    var file = new File();
    file.setId(id);
    file.setFileStatus(FileStatus.UPLOADING);
    file.setFileType(FileType.OTHER);
    file.setMimeType("text/plain");
    file.setOriginalFilename("test.txt");
    file.setSizeBytes(1024L);
    file.setCreatedAt(Instant.now());
    file.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
    return file;
  }
}
