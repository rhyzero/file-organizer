package com.organizer.drive_backend.controller;

import com.organizer.drive_backend.service.BatchProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

@RestController
@RequestMapping("/process")
public class ProcessController {
    @Autowired
    private BatchProcessService batchProcessService;

    @GetMapping("/drive-files")
    public Map<String, String> processDriveFolder() throws IOException, GeneralSecurityException {
        return batchProcessService.batchProcess();
    }
}
