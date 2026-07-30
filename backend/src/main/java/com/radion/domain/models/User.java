package com.radion.domain.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String avatarUrl;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(length = 2048)
    @Convert(converter = TokenEncryptionConverter.class)
    private String googleAccessToken;
    
    @Column(length = 2048)
    @Convert(converter = TokenEncryptionConverter.class)
    private String googleRefreshToken;
    
    private LocalDateTime googleTokenExpiresAt;
}