package com.organizer.drive_backend.controller;

import com.organizer.drive_backend.repository.DocumentRepository;
import com.organizer.drive_backend.service.FirebaseAuthService;
import com.organizer.drive_backend.service.UserFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
