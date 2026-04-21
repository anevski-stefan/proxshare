package com.example.backend.web.exceptions;

public class InvalidFileIdException extends RuntimeException {

  public InvalidFileIdException() {
    super("Invalid file ID format.");
  }
}
