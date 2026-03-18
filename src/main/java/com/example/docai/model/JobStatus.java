package com.example.docai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatus {
    private String jobId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private DocumentMetadata result;
    private String errorMessage;
    private String fileName;
}
