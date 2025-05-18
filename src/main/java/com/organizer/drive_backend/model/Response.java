package com.organizer.drive_backend.model;

import lombok.Data;

@Data
public class Response {
    private int status;
    private String message;
    private String url;
}
