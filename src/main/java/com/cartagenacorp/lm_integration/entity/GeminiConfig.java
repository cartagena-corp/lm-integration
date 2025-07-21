package com.cartagenacorp.lm_integration.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "gemini_config")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "gemini_key", unique = true)
    private String key;

    private String url;
}
