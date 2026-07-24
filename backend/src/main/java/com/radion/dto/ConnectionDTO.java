package com.radion.dto;

import com.radion.domain.enums.Platform;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ConnectionDTO {
    private Platform platform;
    private String status;
}