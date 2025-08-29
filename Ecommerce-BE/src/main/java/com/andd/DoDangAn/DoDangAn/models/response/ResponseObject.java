package com.andd.DoDangAn.DoDangAn.models.response;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public class ResponseObject {
    private HttpStatus status;
    private String message;
    private Object data;

    // Getters
    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}