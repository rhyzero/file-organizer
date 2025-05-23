package com.organizer.drive_backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "document_extractions")
public class DocumentExtraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String fileId;

    private String mimeType;

    @Column(length = 10485760)
    private String extractedText;

    private LocalDateTime extractionTime;

    private String status;

    @Column(length = 1000)  //Store comma-separated tags
    private String tags;

    @Column(columnDefinition = "TEXT")  //Store detailed classification as JSON
    private String tagClassification;
}
