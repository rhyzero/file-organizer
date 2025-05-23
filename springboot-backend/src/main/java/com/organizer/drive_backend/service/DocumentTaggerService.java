package com.organizer.drive_backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DocumentTaggerService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${document.classifier.url}")
    private String classifierUrl;

    public DocumentTaggerService() {
        //Rest Template used because Python backend is a REST API
        this.restTemplate = new RestTemplate();

        //Used to convert Java objects to JSON and vice versa
        this.objectMapper = new ObjectMapper();
    }

    //Function to turn text into JSON to send to Python
    public Map<String, Object> classifyDocument(String text) {
        HttpHeaders headers = new HttpHeaders();

        //Set request content type to JSON
        headers.setContentType(MediaType.APPLICATION_JSON);

        //Attributes in the request body will be String and String
        //Map will be turned into JSON
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("text", text);

        //HttpEntity request will take the requestBody and headers from above
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        //Try to post the JSON request at the classifer URL
        try {
            Map<String, Object> response = restTemplate.postForObject(
                    classifierUrl, request, Map.class);

            //Return the response from the Python backend
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            System.err.println("Error calling classification service: " + e.getMessage());
        }

        //Return empty result if classification failed
        Map<String, Object> emptyResult = new HashMap<>();
        emptyResult.put("primary_tags", List.of());
        emptyResult.put("secondary_tags", List.of());
        return emptyResult;
    }

    //Function to turn JSON object of tags to a single string that contains all the tags separated by commas
    public String getTagString(Map<String, Object> classificationResult) {
        StringBuilder tagBuilder = new StringBuilder();

        //Add primary tags (limited to top 1)
        @SuppressWarnings("unchecked")
        List<String> primaryTags = (List<String>) classificationResult.getOrDefault("primary_tags", List.of());
        if (!primaryTags.isEmpty()) {
            tagBuilder.append(String.join(",", primaryTags));
        }

        //Add secondary tags (limited to top 2)
        @SuppressWarnings("unchecked")
        List<String> secondaryTags = (List<String>) classificationResult.getOrDefault("secondary_tags", List.of());
        if (!secondaryTags.isEmpty()) {
            if (tagBuilder.length() > 0) {
                tagBuilder.append(",");
            }
            //Limit to at most 2 secondary tags
            List<String> limitedSecondaryTags = secondaryTags.size() > 2 ?
                    secondaryTags.subList(0, 2) : secondaryTags;
            tagBuilder.append(String.join(",", limitedSecondaryTags));
        }

        return tagBuilder.toString();
    }

    //Function to turn all tags into a single list
    public List<String> getMainTags(Map<String, Object> classificationResult) {
        List<String> allTags = new ArrayList<>();

        //Add primary tags
        @SuppressWarnings("unchecked")
        //getOrDefault returns empty list if key doesn't exist
        List<String> primaryTags = (List<String>) classificationResult.getOrDefault("primary_tags", List.of());
        allTags.addAll(primaryTags);

        //Add secondary tags
        @SuppressWarnings("unchecked")
        List<String> secondaryTags = (List<String>) classificationResult.getOrDefault("secondary_tags", List.of());

        //Calculate how many secondary tags we can add (max 3 total tags)
        int remainingSlots = 3 - allTags.size();
        if (remainingSlots > 0 && !secondaryTags.isEmpty()) {
            int tagsToAdd = Math.min(remainingSlots, secondaryTags.size());
            allTags.addAll(secondaryTags.subList(0, tagsToAdd));
        }

        return allTags;
    }

    //Function to change the classification result to a JSON string
    public String getTagJson(Map<String, Object> classificationResult) {
        try {
            return objectMapper.writeValueAsString(classificationResult);
        } catch (Exception e) {
            System.err.println("Error turning tags to JSON: " + e.getMessage());
            return "{}";
        }
    }

    public boolean isAcademicDocument(Map<String, Object> classificationResult) {
        //Check document_type field if available
        if (classificationResult.containsKey("document_type")) {
            String documentType = (String) classificationResult.get("document_type");
            if ("academic".equals(documentType)) {
                return true;
            }
        }

        //Get a more focused set of tags
        List<String> mainTags = getMainTags(classificationResult);

        //Check if any academic tags are present
        for (String tag : mainTags) {
            if (List.of("math-science", "humanities", "computer",
                    "business-studies", "arts", "assignment").contains(tag)) {
                return true;
            }
        }

        return false;
    }
}