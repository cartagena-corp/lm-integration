package com.cartagenacorp.lm_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link com.cartagenacorp.lm_integration.entity.ApiUsageLog}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiUsageLogDto implements Serializable {
    private Long id;
    private String feature;
    private UUID projectId;
    private UUID userId;
    private String userEmail;
    private LocalDateTime timestamp;
    private Long responseTimeMs;
    private String status;
}