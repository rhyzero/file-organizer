package com.organizer.drive_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

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
}
