package com.radion.service.pipeline.placement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacementExtractionDTO {
    private String company;
    private String role;
    private String employmentType;
    private String stage;
    private String deadline;
    private String interviewDate;
    private String assessmentDate;
    private String location;
    private String salary;
    private String eligibility;
    private String registrationLink;
    private boolean actionRequired;
    private String priority;
    private Double confidence;
}
