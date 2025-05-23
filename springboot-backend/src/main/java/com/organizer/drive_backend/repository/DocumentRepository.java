package com.organizer.drive_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.organizer.drive_backend.model.DocumentExtraction;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentExtraction, Long> {
    //Find document by Google Drive file ID
    DocumentExtraction findByFileId(String fileId);

    //Find documents by tag (using LIKE with comma separation)
    @Query("SELECT d FROM DocumentExtraction d WHERE d.tags LIKE CONCAT('%', :tag, '%')")
    List<DocumentExtraction> findByTag(@Param("tag") String tag);

    //Find documents by multiple tags (documents must have ALL specified tags)
    @Query(value = "SELECT * FROM document_extractions d WHERE " +
            "(:tag1 IS NULL OR d.tags LIKE CONCAT('%', :tag1, '%')) AND " +
            "(:tag2 IS NULL OR d.tags LIKE CONCAT('%', :tag2, '%')) AND " +
            "(:tag3 IS NULL OR d.tags LIKE CONCAT('%', :tag3, '%'))",
            nativeQuery = true)
    List<DocumentExtraction> findByMultipleTags(
            @Param("tag1") String tag1,
            @Param("tag2") String tag2,
            @Param("tag3") String tag3);

    //Advanced search with both tag and content
    @Query("SELECT d FROM DocumentExtraction d WHERE " +
            "(d.tags LIKE CONCAT('%', :tag, '%')) AND " +
            "(d.extractedText LIKE CONCAT('%', :keyword, '%'))")
    List<DocumentExtraction> findByTagAndKeyword(
            @Param("tag") String tag,
            @Param("keyword") String keyword);

    //Find all academic documents
    @Query("SELECT d FROM DocumentExtraction d WHERE " +
            "d.tags LIKE CONCAT('%', 'math-science', '%') OR " +
            "d.tags LIKE CONCAT('%', 'humanities', '%') OR " +
            "d.tags LIKE CONCAT('%', 'computer', '%') OR " +
            "d.tags LIKE CONCAT('%', 'business-studies', '%') OR " +
            "d.tags LIKE CONCAT('%', 'arts', '%') OR " +
            "d.tags LIKE CONCAT('%', 'assignment', '%')")
    List<DocumentExtraction> findAcademicDocuments();

    //Find documents by professional category
    @Query("SELECT d FROM DocumentExtraction d WHERE " +
            "d.tags NOT LIKE CONCAT('%', 'math-science', '%') AND " +
            "d.tags NOT LIKE CONCAT('%', 'humanities', '%') AND " +
            "d.tags NOT LIKE CONCAT('%', 'computer', '%') AND " +
            "d.tags NOT LIKE CONCAT('%', 'business-studies', '%') AND " +
            "d.tags NOT LIKE CONCAT('%', 'arts', '%') AND " +
            "d.tags NOT LIKE CONCAT('%', 'assignment', '%')")
    List<DocumentExtraction> findProfessionalDocuments();

    //Find documents by academic subject area
    @Query("SELECT d FROM DocumentExtraction d WHERE d.tags LIKE CONCAT('%', :subject, '%')")
    List<DocumentExtraction> findByAcademicSubject(@Param("subject") String subject);
}