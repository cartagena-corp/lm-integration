package com.cartagenacorp.lm_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link com.cartagenacorp.lm_integration.entity.GeminiConfig}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiConfigDto implements Serializable {
    private UUID id;
    private String key;
    private String url;
}