package com.radion.dto;

import lombok.Data;

@Data
public class UserSyncRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String googleAccessToken;
    private String googleRefreshToken;
    private Long googleTokenExpiresAt;
}
