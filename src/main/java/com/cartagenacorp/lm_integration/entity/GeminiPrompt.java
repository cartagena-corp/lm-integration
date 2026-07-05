package com.cartagenacorp.lm_integration.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "gemini_prompt")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiPrompt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "description", length = 20000, nullable = false)
    private String description;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
}
