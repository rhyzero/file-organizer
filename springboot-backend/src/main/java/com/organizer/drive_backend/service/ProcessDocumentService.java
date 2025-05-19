package com.organizer.drive_backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Service
public class ProcessDocumentService {
    //Method to extract text from files based on file type
    public String extractText(File file, String contentType) throws IOException {
        //If type is pdf
        if (contentType.equals("application/pdf")) {
            return extractTextFromPdf(file);

        //If type is doc
        } else if (contentType.equals("application/msword")) {
            return extractTextFromDoc(file);

        //If type is docx
        } else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return extractTextFromDocx(file);
        }
        else {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    //Method to extract text from docx files
    private String extractTextFromDocx(File file) throws IOException {
        //Read bytes from file
        //Create a XWPF object using the stream of bytes that were read
        try (FileInputStream fis = new FileInputStream(file);
            XWPFDocument document = new XWPFDocument(fis)) {

            //Create an extractor to extract the text from the XWPF object
            XWPFWordExtractor wordExtractor = new XWPFWordExtractor(document);

            return wordExtractor.getText();
        }
    }

    private String extractTextFromDoc(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
            HWPFDocument document = new HWPFDocument(fis)) {
            WordExtractor wordExtractor = new WordExtractor(document);

            return wordExtractor.getText();
        }
    }

    private String extractTextFromPdf(File file) throws IOException {
        //Load PDF file using Loader class
        try (PDDocument document = Loader.loadPDF(file)) {
            //Create a text stripper (similar to extractor for doc/docx)
            PDFTextStripper stripper = new PDFTextStripper();

            //Strip text from entire document
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            return stripper.getText(document);
        }
    }
}
