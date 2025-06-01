package com.organizer.drive_backend.controller;

import com.organizer.drive_backend.service.FirebaseAuthService;
import com.organizer.drive_backend.service.UserFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FileContentController {
    @Autowired
    private UserFileService userFileService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @GetMapping("/files/{fileId}/content")
    public ResponseEntity<?> getFileContent(@PathVariable String fileId,
                                            @RequestHeader("Authorization") String authHeader) {
        String firebaseUID;
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid Authorization header");
            }

            String idToken = authHeader.replace("Bearer ", "");
            firebaseUID = firebaseAuthService.verifyTokenAndGetUID(idToken);

            System.out.println("File content request for: " + fileId + " by user: " + firebaseUID);
        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication failed: " + e.getMessage());
        }

        try {
            //Get file content as byte array
            byte[] fileContent = userFileService.getFileContent(firebaseUID, fileId);

            if (fileContent == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File not found or permission denied");
            }

            //Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(fileContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);

        } catch (Exception e) {
            System.err.println("Error fetching file content: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching file content: " + e.getMessage());
        }
    }
}
