package com.organizer.drive_backend.controller;

import com.organizer.drive_backend.repository.DocumentRepository;
import com.organizer.drive_backend.service.FirebaseAuthService;
import com.organizer.drive_backend.service.UserFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FileController {
    @Autowired
    private UserFileService userFileService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @GetMapping("/files")
    public Object getUserFiles(@RequestHeader("Authorization") String authHeader) {
        String firebaseUID;
        try {
            //Check if user is authorized
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Missing or invalid Authorization header";
            }

            String idToken = authHeader.replace("Bearer ", "");
            firebaseUID = firebaseAuthService.verifyTokenAndGetUID(idToken);

            System.out.println("Fetching files for user: " + firebaseUID);
        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            return "Authentication failed: " + e.getMessage();
        }

        try {
            List<Map<String, Object>> userFiles = userFileService.getUserFiles(firebaseUID);
            System.out.println("Found " + userFiles.size() + " files for user: " + firebaseUID);
            return userFiles;

        } catch (Exception e) {
            System.err.println("Error fetching user files: " + e.getMessage());
            return "Error fetching files: " + e.getMessage();
        }
    }

    @DeleteMapping("/files/{fileId}")
    public Object deleteFile(@PathVariable String fileId,
                             @RequestHeader("Authorization") String authHeader) {
        String firebaseUID;
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Missing or invalid Authorization header";
            }

            String idToken = authHeader.replace("Bearer ", "");
            firebaseUID = firebaseAuthService.verifyTokenAndGetUID(idToken);

            System.out.println("Delete request for file: " + fileId + " by user: " + firebaseUID);
        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            return "Authentication failed: " + e.getMessage();
        }

        try {
            boolean success = userFileService.deleteFile(firebaseUID, fileId);

            //Return response model with deleted fileId
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "File deleted successfully");
                response.put("fileId", fileId);
                return response;
            } else {
                return "File not found or permission denied";
            }

        } catch (Exception e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return "Error deleting file: " + e.getMessage();
        }
    }

    @PutMapping("/files/{fileId}/rename")
    public Object renameFile(@PathVariable String fileId,
                             @RequestBody Map<String, String> requestBody,
                             @RequestHeader("Authorization") String authHeader) {

        String firebaseUID;
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Missing or invalid Authorization header";
            }

            String idToken = authHeader.replace("Bearer ", "");
            firebaseUID = firebaseAuthService.verifyTokenAndGetUID(idToken);

            System.out.println("Rename request for file: " + fileId + " by user: " + firebaseUID);

        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            return "Authentication failed: " + e.getMessage();
        }

        //Get new filename from request body
        String newFileName = requestBody.get("fileName");
        if (newFileName == null || newFileName.trim().isEmpty()) {
            return "New filename is required";
        }

        try {
            Map<String, Object> updatedFile = userFileService.renameFile(firebaseUID, fileId, newFileName.trim());

            if (updatedFile != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "File renamed successfully");
                response.put("file", updatedFile);
                return response;
            } else {
                return "File not found or permission denied";
            }

        } catch (Exception e) {
            System.err.println("Error renaming file: " + e.getMessage());
            return "Error renaming file: " + e.getMessage();
        }
    }
}
