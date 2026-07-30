package com.radion.domain.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_processing_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String extractedJson;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    private Double confidenceScore;

    @CreationTimestamp
    private LocalDateTime processedAt;
}