package com.example.docai.service;

import com.example.docai.model.DocumentMetadata;
import com.example.docai.model.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final GeminiService geminiService;
    private final Map<String, JobStatus> jobs = new ConcurrentHashMap<>();

    public String submitJob(byte[] fileBytes, String contentType, String fileName) {
        String jobId = java.util.UUID.randomUUID().toString();
        JobStatus job = JobStatus.builder()
                .jobId(jobId)
                .status("PENDING")
                .fileName(fileName)
                .build();
        jobs.put(jobId, job);

        log.info("Job {} submitted for file {}", jobId, fileName);
        processAsync(jobId, fileBytes, contentType);
        return jobId;
    }

    @Async
    public void processAsync(String jobId, byte[] fileBytes, String contentType) {
        JobStatus job = jobs.get(jobId);
        job.setStatus("PROCESSING");
        log.info("Processing job {}", jobId);

        try {
            DocumentMetadata metadata = geminiService.processDocument(fileBytes, contentType);
            
            // Validation Layer
            if (validateMetadata(metadata)) {
                job.setStatus("COMPLETED");
                job.setResult(metadata);
                log.info("Job {} completed successfully", jobId);
            } else {
                job.setStatus("FAILED");
                job.setErrorMessage("One or more required fields (name, policy_number, date) could not be extracted.");
                log.warn("Job {} failed validation: {}", jobId, metadata);
            }
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            log.error("Job {} failed: {}", jobId, e.getMessage());
        }
    }

    private boolean validateMetadata(DocumentMetadata metadata) {
        return metadata.getName() != null && !metadata.getName().isEmpty() &&
               metadata.getPolicyNumber() != null && !metadata.getPolicyNumber().isEmpty() &&
               metadata.getDate() != null && !metadata.getDate().isEmpty();
    }

    public JobStatus getJobStatus(String jobId) {
        return jobs.get(jobId);
    }
}
