package com.task.file.controller;

import com.fasterxml.jackson.annotation.JsonInclude;

public class Response {
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String debugMessage;

    public Response() {
    }

    public Response(String message) {
        this.message = message;
    }

    public Response(String message, String debugMessage) {
        this.message = message;
        this.debugMessage = debugMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
