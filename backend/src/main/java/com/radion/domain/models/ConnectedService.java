package com.radion.domain.models;

import com.radion.domain.enums.ConnectionStatus;
import com.radion.domain.enums.Platform;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "connected_services")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConnectedService {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    private Platform platform;
    
    @Enumerated(EnumType.STRING)
    private ConnectionStatus status;
    
    private LocalDateTime lastSyncAt;

    @Column(length = 2048)
    @Convert(converter = TokenEncryptionConverter.class)
    private String accessToken;
    
    @Column(length = 2048)
    @Convert(converter = TokenEncryptionConverter.class)
    private String refreshToken;
    
    private LocalDateTime tokenExpiresAt;
    private String externalAccountId;
    private String accountEmail;
    private String accountName;
    private String accountAvatarUrl;
    
    @Column(length = 2048)
    private String grantedScopes;
}