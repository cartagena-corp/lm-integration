package com.cartagenacorp.lm_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link com.cartagenacorp.lm_integration.entity.GeminiPrompt}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiPromptDTO implements Serializable {
    private UUID id;
    private String title;
    private String description;
    private UUID organizationId;
}