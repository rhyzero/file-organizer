package com.organizer.drive_backend.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.organizer.drive_backend.model.DocumentExtraction;
import com.organizer.drive_backend.model.Response;
import com.organizer.drive_backend.repository.DocumentRepository;

@Service
public class UploadService {
    @Autowired
    private ProcessDocumentService documentProcessor;

    @Autowired
    private DocumentTaggerService documentTaggerService;

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

    //Method to upload files to drive
    public Response uploadFileToDrive(File file, String contentType, String originalFilename) throws GeneralSecurityException, IOException {
        //Response object to store result of the upload operation
        Response response = new Response();

        //Text extraction and classification
        String extractedText = null;
        Map<String, Object> classificationResult = Map.of(); //Default empty map
        String tagString = "";
        String tagJson = "{}";

        try {
            extractedText = documentProcessor.extractText(file, contentType);
            System.out.println("Successfully extracted text, length: " + extractedText.length());

            //Classify the document using our simplified tag structure
            if (extractedText != null && !extractedText.isEmpty()) {
                classificationResult = documentTaggerService.classifyDocument(extractedText);
                tagString = documentTaggerService.getTagString(classificationResult);
                tagJson = documentTaggerService.getTagJson(classificationResult);

                System.out.println("Document classified with tags: " + tagString);

                //Check if it's an academic document
                boolean isAcademic = documentTaggerService.isAcademicDocument(classificationResult);
                System.out.println("Document type: " + (isAcademic ? "academic" : "professional"));
            }
        } catch (Exception e) {
            System.err.println("Error extracting or classifying text: " + e.getMessage());
            //Continue with upload even if text extraction fails
        }

        try {
            //Create a Google Drive instance
            Drive drive = createDriveService();

            //Create metadata for the file we are uploading
            com.google.api.services.drive.model.File fileMetaData = new com.google.api.services.drive.model.File();

            //Set filename of the file on Google Drive to match our local filename
            fileMetaData.setName(file.getName());

            //Set the parent folder where the file will reside
            fileMetaData.setParents(Collections.singletonList(folderId));

            //Create a filecontent object with the document's content type (file type) and the file itself
            FileContent mediaContent = new FileContent(contentType, file);

            //Execute the upload request
            com.google.api.services.drive.model.File uploadedFile = drive.files().create(fileMetaData, mediaContent).setFields("id").execute();

            //Construct a url to match the file url in Google Drive
            String fileUrl = "https://drive.google.com/file/d/" + uploadedFile.getId() + "/view";

            //Print out the file url
            System.out.println("Document URL: " + fileUrl);

            //Store document information with classification in the database
            DocumentExtraction extraction = new DocumentExtraction();
            extraction.setFileName(originalFilename);
            extraction.setFileId(uploadedFile.getId());
            extraction.setMimeType(contentType);
            extraction.setExtractedText(extractedText);
            extraction.setExtractionTime(LocalDateTime.now());
            extraction.setStatus("Success");
            extraction.setTags(tagString);
            extraction.setTagClassification(tagJson);
            documentRepository.save(extraction);

            //Set status for operation
            response.setStatus(200);

            //Set message for operation
            response.setMessage("Document Successfully Uploaded to Drive");

            //Set url for uploaded file
            response.setUrl(fileUrl);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            response.setStatus(500);
            response.setMessage(e.getMessage());
        }
        return response;
    };

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