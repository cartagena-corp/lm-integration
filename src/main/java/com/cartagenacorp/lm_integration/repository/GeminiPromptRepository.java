package com.cartagenacorp.lm_integration.repository;

import com.cartagenacorp.lm_integration.entity.GeminiPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeminiPromptRepository extends JpaRepository<GeminiPrompt, UUID> {
    List<GeminiPrompt> findByOrganizationId(UUID organizationId);
}