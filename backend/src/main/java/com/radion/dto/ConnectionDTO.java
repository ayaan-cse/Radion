package com.radion.dto;

import com.radion.domain.enums.Platform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConnectionDTO {
    private Platform platform;
    private String status;
    private String lastSyncAt;
    private String accountEmail;
    private String accountName;
    private String accountAvatarUrl;
}