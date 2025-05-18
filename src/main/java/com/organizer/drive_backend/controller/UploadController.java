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

@RestController
public class UploadController {
    //Automatically inject service
    @Autowired
    private UploadService uploadService;

    //POST Endpoint
    @PostMapping("/upload")
    public Object handleFileUpload(@RequestParam("document")MultipartFile file) throws IOException, GeneralSecurityException {
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

        //Create a temporary file on the server's filesystem to store uploaded content
        File tempFile = File.createTempFile("temp", null);

        //Transfer contents of uploaded file to the temp file
        file.transferTo(tempFile);

        //Execute the upload service method to upload the file
        Response response = uploadService.uploadFileToDrive(tempFile, contentType);
        System.out.println(response);
        return response;
    }
}
