package com.organizer.drive_backend.controller;

import com.organizer.drive_backend.model.Response;
import com.organizer.drive_backend.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.web.bind.annotation.RequestHeader;
import com.organizer.drive_backend.service.FirebaseAuthService;

@RestController
public class UploadController {
    //Automatically inject service
    @Autowired
    private UploadService uploadService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    //POST Endpoint
    @PostMapping("/upload")
    public Object handleFileUpload(@RequestParam("document")MultipartFile file,
                                   @RequestHeader("Authorization") String authHeader) throws IOException, GeneralSecurityException {
        //Firebase Authentication
        String firebaseUID;
        try {
            //Check if request comes with correct header and in the correct format
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "Missing or invalid Authorization header";
            }

            //Removes the 'bearer' prefix to get only the firebase token
            String idToken = authHeader.replace("Bearer ", "");

            //Verify token and get Firebase UID
            firebaseUID = firebaseAuthService.verifyTokenAndGetUID(idToken);
            System.out.println("Authenticated user UID: " + firebaseUID);

        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            return "Authentication failed: " + e.getMessage();
        }

        //Check if file is empty
        if (file.isEmpty()) {
            return "File is not valid";
        }

        //Get the file content type to verify if it is supported
        String contentType = file.getContentType();

        //Check if file is accepted (PDF, or doc/docx files)
        if (contentType == null || !(contentType.equals("application/pdf") ||
                contentType.equals("application/msword") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            return "Only PDF and Word documents are allowed";
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            // Fallback extensions based on content type
            if (contentType.equals("application/pdf")) {
                extension = ".pdf";
            } else if (contentType.equals("application/msword")) {
                extension = ".doc";
            } else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                extension = ".docx";
            }
        }

        //Create a temporary file on the server's filesystem to store uploaded content
        File tempFile = File.createTempFile("temp", extension);

        System.out.println("Created temp file: " + tempFile.getAbsolutePath() +
                " for original file: " + originalFilename);

        //Transfer contents of uploaded file to the temp file
        file.transferTo(tempFile);

        //Execute the upload service method to upload the file
        try {
            //Execute the upload service method to upload the file (now with Firebase UID)
            Response response = uploadService.uploadFileToDrive(tempFile, contentType, originalFilename, firebaseUID);
            System.out.println("Upload response: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("Upload failed: " + e.getMessage());
            return "Upload failed: " + e.getMessage();
        } finally {
            // Clean up temporary file
            if (tempFile.exists() && !tempFile.delete()) {
                System.err.println("Failed to delete temporary file: " + tempFile.getAbsolutePath());
            }
        }
    }
}
