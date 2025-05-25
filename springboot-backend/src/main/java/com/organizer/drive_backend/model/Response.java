package com.organizer.drive_backend.model;

import java.util.List;

import lombok.Data;

@Data
public class Response {
    private int status;
    private String message;
    private String url;
    private String fileName;
    private List<String> tags;
    private String documentType;
    private Double confidence;
}