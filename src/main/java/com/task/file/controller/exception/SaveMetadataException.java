package com.task.file.controller.exception;

public class SaveMetadataException extends RuntimeException{
    public SaveMetadataException() {
        super();
    }
    public SaveMetadataException(String message, Throwable cause) {
        super(message, cause);
    }
    public SaveMetadataException(String message) {
        super(message);
    }
    public SaveMetadataException(Throwable cause) {
        super(cause);
    }
}
