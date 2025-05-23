package com.organizer.drive_backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.drive_backend.model.DocumentExtraction;
import com.organizer.drive_backend.repository.DocumentRepository;

@RestController
@RequestMapping("/tags")
public class DocumentController {

    @Autowired
    private DocumentRepository documentRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${document.classifier.url}")
    private String classifierBaseUrl;

    @GetMapping("/supported")
    public ResponseEntity<Map<String, Object>> getSupportedTags() {
        try {
            //Get supported tags from the Python service
            Map<String, Object> response = restTemplate.getForObject(
                    classifierBaseUrl + "/supported-tags", Map.class);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve supported tags: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    //Endpoint to get all documents with a given tag
    @GetMapping("/by-tag/{tag}")
    public ResponseEntity<List<Map<String, Object>>> getDocumentsByTag(@PathVariable String tag) {
        List<DocumentExtraction> documents = documentRepository.findByTag(tag);

        List<Map<String, Object>> result = documents.stream()
                .map(this::convertToDocumentInfo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchDocuments(
            @RequestParam(required = false) String tag1,
            @RequestParam(required = false) String tag2,
            @RequestParam(required = false) String tag3,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {

        List<DocumentExtraction> documents;

        //First handle type filter if specified
        if (type != null) {
            if (type.equalsIgnoreCase("academic")) {
                documents = documentRepository.findAcademicDocuments();
            } else if (type.equalsIgnoreCase("professional")) {
                documents = documentRepository.findProfessionalDocuments();
            } else {
                documents = documentRepository.findAll();
            }
        } else if (tag1 != null || tag2 != null || tag3 != null) {
            //Search by multiple tags
            documents = documentRepository.findByMultipleTags(tag1, tag2, tag3);
        } else if (keyword != null && !keyword.isEmpty()) {
            //Search by text content only (with no specific tag)
            documents = documentRepository.findAll().stream()
                    .filter(doc -> doc.getExtractedText() != null &&
                            doc.getExtractedText().contains(keyword))
                    .collect(Collectors.toList());
        } else {
            //Return empty result if no search criteria provided
            documents = List.of();
        }

        //Apply additional keyword filter if both tags and keyword specified
        if (keyword != null && !keyword.isEmpty() &&
                (tag1 != null || tag2 != null || tag3 != null || type != null)) {
            documents = documents.stream()
                    .filter(doc -> doc.getExtractedText() != null &&
                            doc.getExtractedText().contains(keyword))
                    .collect(Collectors.toList());
        }

        //Create a stream from the list of filtered documents
        //Convert each DocumentExtraction object into a Map using the convertToDocumentInfo function
        //Collect all changed Maps into a list
        List<Map<String, Object>> result = documents.stream()
                .map(this::convertToDocumentInfo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search/academic")
    public ResponseEntity<List<Map<String, Object>>> searchAcademicDocuments(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String keyword) {

        List<DocumentExtraction> documents;

        //If specific subject is provided, use it
        if (subject != null && !subject.isEmpty()) {
            documents = documentRepository.findByAcademicSubject(subject);
        } else {
            //Otherwise, get all academic documents
            documents = documentRepository.findAcademicDocuments();
        }

        //Further filter by keyword if provided
        if (keyword != null && !keyword.isEmpty()) {
            documents = documents.stream()
                    .filter(doc -> doc.getExtractedText() != null &&
                            doc.getExtractedText().contains(keyword))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = documents.stream()
                .map(this::convertToDocumentInfo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, List<String>>> getTagCategories() {
        Map<String, List<String>> categories = new HashMap<>();

        categories.put("professional", List.of(
                "legal", "financial", "technical", "marketing", "hr", "strategic",
                "research", "policy", "report", "product", "customer",
                "correspondence", "administrative", "compliance"
        ));

        categories.put("academic", List.of(
                "math-science", "humanities", "computer", "business-studies", "arts", "assignment"
        ));

        return ResponseEntity.ok(categories);
    }

    @PutMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> updateDocumentTags(
            @PathVariable Long documentId,
            @RequestBody Map<String, Object> tagUpdate) {

        DocumentExtraction document = documentRepository.findById(documentId)
                .orElse(null);

        if (document == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Document not found");
            return ResponseEntity.status(404).body(errorResponse);
        }

        try {
            //Update tags
            if (tagUpdate.containsKey("tags")) {
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) tagUpdate.get("tags");
                document.setTags(String.join(",", tags));
            }

            //Update tag classification JSON
            if (tagUpdate.containsKey("tagClassification")) {
                String tagJson = objectMapper.writeValueAsString(tagUpdate.get("tagClassification"));
                document.setTagClassification(tagJson);
            }

            documentRepository.save(document);

            return ResponseEntity.ok(convertToDocumentInfo(document));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update tags: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    //Convert database entry to a Map for API usage
    private Map<String, Object> convertToDocumentInfo(DocumentExtraction document) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", document.getId());
        info.put("fileName", document.getFileName());
        info.put("fileId", document.getFileId());
        info.put("mimeType", document.getMimeType());

        if (document.getExtractionTime() != null) {
            info.put("extractionTime", document.getExtractionTime().toString());
        }

        //Convert tags string to array
        if (document.getTags() != null && !document.getTags().isEmpty()) {
            info.put("tags", document.getTags().split(","));
        } else {
            info.put("tags", new String[0]);
        }

        //Generate Drive URL
        info.put("url", "https://drive.google.com/file/d/" + document.getFileId() + "/view");

        //Parse tag classification JSON if available
        try {
            if (document.getTagClassification() != null && !document.getTagClassification().isEmpty()) {
                Map<String, Object> classification = objectMapper.readValue(
                        document.getTagClassification(), Map.class);
                info.put("tagClassification", classification);

                //Determine document type (professional or academic)
                if (classification.containsKey("document_type")) {
                    info.put("documentType", classification.get("document_type"));
                } else {
                    //Fallback by checking tags
                    boolean isAcademic = false;
                    String[] tags = document.getTags().split(",");
                    for (String tag : tags) {
                        if (List.of("math-science", "humanities", "computer",
                                "business-studies", "arts", "assignment").contains(tag)) {
                            isAcademic = true;
                            break;
                        }
                    }
                    info.put("documentType", isAcademic ? "academic" : "professional");
                }
            }
        } catch (Exception e) {
            //If JSON parsing fails, include the raw string
            info.put("tagClassification", document.getTagClassification());
        }

        return info;
    }
}