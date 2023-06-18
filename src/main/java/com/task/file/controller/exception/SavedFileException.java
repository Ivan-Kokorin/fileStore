package com.task.file.controller.exception;

public class SavedFileException extends RuntimeException{
    public SavedFileException() {
        super();
    }
    public SavedFileException(String message, Throwable cause) {
        super(message, cause);
    }
    public SavedFileException(String message) {
        super(message);
    }
    public SavedFileException(Throwable cause) {
        super(cause);
    }
}
