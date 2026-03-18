package com.example.docai.controller;

import com.example.docai.model.JobStatus;
import com.example.docai.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentUploadController {

    private final DocumentProcessingService processingService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocuments(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "No files uploaded"));
        }

        java.util.List<String> jobIds = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String jobId = processingService.submitJob(file.getBytes(), file.getContentType(), file.getOriginalFilename());
                jobIds.add(jobId);
            } catch (Exception e) {
                log.error("Upload failed for file {}", file.getOriginalFilename(), e);
            }
        }
        return ResponseEntity.ok(Collections.singletonMap("jobIds", jobIds));
    }

    @PostMapping("/upload-single")
    public ResponseEntity<Map<String, String>> uploadSingleDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "File is empty"));
        }

        try {
            String jobId = processingService.submitJob(file.getBytes(), file.getContentType(), file.getOriginalFilename());
            return ResponseEntity.ok(Collections.singletonMap("jobId", jobId));
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatus> getStatus(@PathVariable String jobId) {
        JobStatus status = processingService.getJobStatus(jobId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
