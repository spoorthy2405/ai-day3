package com.example.docai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {
    private String name;
    private String policyNumber;
    private String date;
    private Double confidence;
}
