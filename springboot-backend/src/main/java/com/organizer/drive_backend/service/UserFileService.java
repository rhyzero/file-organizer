package com.organizer.drive_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.organizer.drive_backend.model.DocumentExtraction;
import com.organizer.drive_backend.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class UserFileService {
    @Autowired
    private DocumentRepository documentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String SERVICE_ACCOUNT_KEY_PATH = getPathToGoogleCredentials();

    @Value("${google.drive.folder.id}")
    private String folderId;

    private static String getPathToGoogleCredentials() {
        //Get current directory of application
        String currentDirectory = System.getProperty("user.dir");

        //Join current directory with filename
        Path filePath = Paths.get(currentDirectory, "cred.json");

        //Return filepath as a string
        return filePath.toString();
    };

    public List<Map<String, Object>> getUserFiles(String firebaseUID) throws GeneralSecurityException, IOException {
        List<Map<String, Object>> userFiles = new ArrayList<>();

        try {
            Drive drive = createDriveService();

            String userFolderId = getUserFolderId(drive, firebaseUID);

            //If user doesn't have a folder
            if (userFolderId == null) {
                return userFiles;
            }

            //Query to search user folder for files
            String query = "'" + userFolderId + "' in parents and trashed=false";
            FileList result = drive.files().list()
                    .setQ(query)
                    //Only return these specific fields
                    .setFields("files(id, name, createdTime, mimeType, size)")
                    .setOrderBy("createdTime desc")
                    .execute();

            //For each file in the user's drive folder:
            //Create a map of the basic info
            //Search for tags in the database using the fileId
            //Add tags if found
            //Mark if tags aren't found
            //Add to the final list of files
            for (com.google.api.services.drive.model.File driveFile : result.getFiles()) {
                Map<String, Object> fileInfo = new HashMap<>();

                fileInfo.put("fileId", driveFile.getId());
                fileInfo.put("fileName", driveFile.getName());
                fileInfo.put("mimeType", driveFile.getMimeType());
                fileInfo.put("driveUrl", "https://drive.google.com/file/d/" + driveFile.getId() + "/view");

                if (driveFile.getCreatedTime() != null) {
                    fileInfo.put("uploadDate", driveFile.getCreatedTime().toString());
                }

                if (driveFile.getSize() != null) {
                    fileInfo.put("size", driveFile.getSize());
                }

                DocumentExtraction dbRecord = documentRepository.findByFileId(driveFile.getId());

                if (dbRecord != null) {
                    if (dbRecord.getTags() != null && !dbRecord.getTags().isEmpty()) {
                        fileInfo.put("tags", dbRecord.getTags().split(","));
                    } else {
                        fileInfo.put("tags", new String[0]);
                    }

                    //If tags are found in the database
                    try {
                        if (dbRecord.getTagClassification() != null && !dbRecord.getTagClassification().isEmpty()) {
                            Map<String, Object> classification = objectMapper.readValue(
                                    dbRecord.getTagClassification(), Map.class);
                            fileInfo.put("tagClassification", classification);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing tag classification JSON: " + e.getMessage());
                    }
                    fileInfo.put("extractionStatus", dbRecord.getStatus());
                    fileInfo.put("processingDate", dbRecord.getExtractionTime() != null ?
                            dbRecord.getExtractionTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                } else {
                    //Mark tags not found
                    fileInfo.put("tags", new String[0]);
                    fileInfo.put("extractionStatus", "Not processed");
                }
                userFiles.add(fileInfo);
            }
        } catch (Exception e) {
            System.err.println("Error retrieving user files: " + e.getMessage());
            throw e;
        }
        return userFiles;
    }

    private String getUserFolderId(Drive drive, String firebaseUID) throws IOException {
        String userFolderName = "user_" + firebaseUID;

        //Search for user's folder
        String query = "'" + folderId + "' in parents and name='" + userFolderName + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";

        FileList result = drive.files().list()
                .setQ(query)
                .setFields("files(id)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        return null;
    }

    private Drive createDriveService() throws GeneralSecurityException, IOException {
        //Load Google credentials from the credential file
        //Scoped to only access Google Drive
        GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(SERVICE_ACCOUNT_KEY_PATH)).createScoped(Collections.singleton(DriveScopes.DRIVE));

        //Create and return a new Drive instance with HTTP, JSON Factory for parsing, Google Credentials
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
                .build();
    }

    //Function to delete a file from the user's drive folder
    public boolean deleteFile(String firebaseUID, String fileId) throws GeneralSecurityException, IOException {
        try {
            Drive drive = createDriveService();

            //Verify the file belongs to this user
            com.google.api.services.drive.model.File driveFile = drive.files().get(fileId)
                    .setFields("parents")
                    .execute();

            String userFolderId = getUserFolderId(drive, firebaseUID);
            if (userFolderId == null || !driveFile.getParents().contains(userFolderId)) {
                System.err.println("File does not belong to user or user folder not found");
                return false;
            }

            //Delete file from Google Drive
            drive.files().delete(fileId).execute();
            System.out.println("File deleted from Google Drive: " + fileId);

            //Delete from database
            DocumentExtraction dbRecord = documentRepository.findByFileId(fileId);
            if (dbRecord != null) {
                documentRepository.delete(dbRecord);
                System.out.println("File deleted from database: " + fileId);
            }

            return true;

        } catch (Exception e) {
            System.err.println("Error deleting file: " + e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> renameFile(String firebaseUID, String fileId, String newFileName) throws GeneralSecurityException, IOException {
        try {
            Drive drive = createDriveService();

            //Verify the file belongs to this user
            com.google.api.services.drive.model.File driveFile = drive.files().get(fileId)
                    .setFields("parents")
                    .execute();

            String userFolderId = getUserFolderId(drive, firebaseUID);
            if (userFolderId == null || !driveFile.getParents().contains(userFolderId)) {
                System.err.println("File does not belong to user or user folder not found");
                return null;
            }

            //Update file name in Google Drive
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(newFileName);

            drive.files().update(fileId, fileMetadata).execute();
            System.out.println("File renamed in Google Drive: " + fileId + " to " + newFileName);

            //Update database record
            DocumentExtraction dbRecord = documentRepository.findByFileId(fileId);
            if (dbRecord != null) {
                dbRecord.setFileName(newFileName);
                documentRepository.save(dbRecord);
                System.out.println("File renamed in database: " + fileId);
            }

            //Return simple success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File renamed successfully");
            response.put("fileId", fileId);
            response.put("newFileName", newFileName);
            return response;

        } catch (Exception e) {
            System.err.println("Error renaming file: " + e.getMessage());
            throw e;
        }
    }
}
