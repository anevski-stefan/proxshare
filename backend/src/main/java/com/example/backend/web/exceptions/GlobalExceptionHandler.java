package com.example.backend.web.exceptions;

import com.example.backend.dto.ErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(FileNotFoundException.class)
  public ResponseEntity<ErrorDTO> handleFileNotFound(FileNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorDTO("NOT_FOUND", "The requested file does not exist."));
  }

  @ExceptionHandler(FileGoneException.class)
  public ResponseEntity<ErrorDTO> handleFileGone(FileGoneException ex) {
    return ResponseEntity.status(HttpStatus.GONE)
        .body(new ErrorDTO("GONE", "This file has expired and is no longer available."));
  }

  @ExceptionHandler(InvalidFileIdException.class)
  public ResponseEntity<ErrorDTO> handleInvalidFileId(InvalidFileIdException ex) {
    return ResponseEntity.badRequest()
        .body(new ErrorDTO("BAD_REQUEST", ex.getMessage()));
  }
}
