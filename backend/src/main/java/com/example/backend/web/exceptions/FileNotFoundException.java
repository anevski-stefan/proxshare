package com.example.backend.web.exceptions;

public class FileNotFoundException extends RuntimeException {

  public FileNotFoundException(String message) {
    super(message);
  }
}
