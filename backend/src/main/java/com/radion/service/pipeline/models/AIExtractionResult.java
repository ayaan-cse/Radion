package com.radion.service.pipeline.models;

import com.radion.domain.enums.EventCategory;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class AIExtractionResult {
    // Classification
    private String classification; // EVENT, TASK, REMINDER, IGNORE
    private EventCategory category; // INTERVIEW, MEETING, DEADLINE, TASK
    
    // Core Entities
    private String companyName;
    private String assignmentName;
    private String subject;
    
    // Placement Specifics
    private String role;
    private String ctc;
    private List<String> eligibilityCriteria;
    private List<String> requiredDocuments;
    private String interviewRounds;
    
    // Temporal Data
    private LocalDate eventDate; // Registration deadline, assessment date, or due date
    private LocalTime eventTime;
    
    // Context & Action
    private String priority; // HIGH, MEDIUM, LOW
    private boolean actionRequired;
    private List<String> actionItems;
    private List<String> meetingLinks;
    private String registrationLink;
    private String location;
    
    // AI Metadata
    private String summary;
    private double confidenceScore;
}