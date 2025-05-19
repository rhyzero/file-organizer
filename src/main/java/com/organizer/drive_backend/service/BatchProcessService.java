package com.organizer.drive_backend.service;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class BatchProcessService {

    @Autowired
    private ProcessDocumentService documentProcessor;

    @Autowired
    private DocumentRepository documentRepository;

    //JSON factory from Google API to parse and generate JSON
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    //Path to Google service account credentials
    private static final String SERVICE_ACCOUNT_KEY_PATH = getPathToGoogleCredentials();

    //ID of folder where files are uploaded
    @Value("${google.drive.folder.id}")
    private String folderId;

    //Method to find filesystem path for credentials file
    //Take current directory as base and then append the filename
    private static String getPathToGoogleCredentials() {
        //Get current directory of application
        String currentDirectory = System.getProperty("user.dir");

        //Join current directory with filename
        Path filePath = Paths.get(currentDirectory, "cred.json");

        //Return filepath as a string
        return filePath.toString();
    };

    //Method to extract text from all files in a drive folder
    public Map<String, String> batchProcess() throws IOException, GeneralSecurityException {
        //HashMap to store results {fileName : resultOfExtraction}
        Map<String, String> extractionResults = new HashMap<>();

        Drive drive = createDriveService();

        //Get a list of files using the following queries
        FileList result = drive.files().list().setQ("'" + folderId + "' in parents and trashed = false")
                .setFields("files(id, name, mimeType)")
                .execute();

        System.out.println("Found files in folder: " + result.getFiles().size());

        //Loop through each file in fileList
        for (com.google.api.services.drive.model.File driveFile : result.getFiles()) {
            String fileId = driveFile.getId();
            String fileName = driveFile.getName();
            String mimeType = driveFile.getMimeType();

            //Check if a file was already processed
            DocumentExtraction existingExtraction = documentRepository.findByFileId(fileId);
            if(existingExtraction != null) {
                extractionResults.put(fileName, "Already processed on: " + existingExtraction.getExtractionTime());
                continue;
            }

            System.out.println("Processing: " + fileName + " (" + mimeType + ")");

            //Download the files to extract
            try {
                File localFile = downloadFile(drive, fileId, fileName);
                String extractedText = documentProcessor.extractText(localFile, mimeType);

                //Create an extraction object and sets their attributes to submit to the database
                DocumentExtraction extraction = new DocumentExtraction();
                extraction.setFileName(fileName);
                extraction.setFileId(fileId);
                extraction.setMimeType(mimeType);
                extraction.setExtractedText(extractedText);
                extraction.setExtractionTime(LocalDateTime.now());
                extraction.setStatus("Success");
                documentRepository.save(extraction);

                extractionResults.put(fileName, "Successfully processed");

                if (!localFile.delete()) {
                    System.out.println("Failed to delete temp file.");
                }

                System.out.println("Extracted text from: " + localFile.getName());
            } catch (Exception e) {
                System.err.println("Error processing file: " + fileName + ": " + e.getMessage());
                DocumentExtraction extraction = new DocumentExtraction();
                extraction.setFileName(fileName);
                extraction.setFileId(fileId);
                extraction.setMimeType(mimeType);
                extraction.setExtractionTime(LocalDateTime.now());
                extraction.setStatus("Failed: " + e.getMessage());
                documentRepository.save(extraction);
            }
        }

        return extractionResults;
    }

    //Method to download the files
    private File downloadFile(Drive drive, String fileId, String fileName) throws IOException {
        String extension = getExtensionFromFileName(fileName);

        //Create the tempFile on the user's device
        File tempFile = File.createTempFile("gdrive_", extension);

        //Write data from drive files to the tempFile
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        }

        return tempFile;
    }

    //Returns the extension of the file
    private String getExtensionFromFileName(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot > 0) {
            return fileName.substring(lastIndexOfDot);
        }
        //Default if no extension found
        return ".tmp";
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
}
