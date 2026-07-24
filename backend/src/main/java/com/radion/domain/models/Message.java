package com.radion.domain.models;

import com.radion.domain.enums.Platform;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    private Platform platform;
    
    private String externalId;
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String rawPayload;
    
    private boolean isUnread;
    private LocalDateTime receivedAt;
}