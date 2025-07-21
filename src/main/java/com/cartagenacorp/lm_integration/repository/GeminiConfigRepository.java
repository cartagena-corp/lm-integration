package com.cartagenacorp.lm_integration.repository;

import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GeminiConfigRepository extends JpaRepository<GeminiConfig, UUID> {
}