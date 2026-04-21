package com.example.backend.web.exceptions;

public class FileGoneException extends RuntimeException {

  public FileGoneException(String message) {
    super(message);
  }
}
