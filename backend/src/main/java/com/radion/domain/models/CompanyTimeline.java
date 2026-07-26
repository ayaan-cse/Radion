package com.radion.domain.models;

import com.radion.domain.enums.TimelineStage;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_timelines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String companyName;

    private String role;
    private String employmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimelineStage currentStage;

    private LocalDateTime lastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_message_id")
    private Message latestMessage;

    private String salary;
    private String location;
    
    @Column(columnDefinition = "TEXT")
    private String eligibility;
    
    @Column(length = 2048)
    private String registrationLink;
}
