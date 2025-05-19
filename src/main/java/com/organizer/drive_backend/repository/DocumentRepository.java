package com.organizer.drive_backend.repository;

import com.organizer.drive_backend.model.DocumentExtraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentExtraction, Long> {
    DocumentExtraction findByFileId(String fileId);
}
