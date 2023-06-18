package com.task.file.controller.exception;

public class ReadInputStreamException extends RuntimeException{
    public ReadInputStreamException() {
        super();
    }
    public ReadInputStreamException(String message, Throwable cause) {
        super(message, cause);
    }
    public ReadInputStreamException(String message) {
        super(message);
    }
    public ReadInputStreamException(Throwable cause) {
        super(cause);
    }
}
