package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.GeminiConfigDto;
import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import com.cartagenacorp.lm_integration.repository.GeminiConfigRepository;
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GeminiConfigService {

    private final GeminiConfigRepository geminiConfigRepository;

    public GeminiConfigService(GeminiConfigRepository geminiConfigRepository) {
        this.geminiConfigRepository = geminiConfigRepository;
    }

    public GeminiConfig getGeminiConfigForInternalUse() {
        UUID organizationId = JwtContextHolder.getOrganizationId();
        GeminiConfig config = geminiConfigRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new RuntimeException("No se encontró configuración de Gemini para la organización " + organizationId));
        return config;
    }

    public GeminiConfig getGeminiConfigForFrontend() {
        UUID organizationId = JwtContextHolder.getOrganizationId();
        GeminiConfig config = geminiConfigRepository.findByOrganizationId(organizationId)
                .orElse(null);

        if (config == null) {
            return new GeminiConfig();
        }

        GeminiConfig configForFrontend = new GeminiConfig();
        configForFrontend.setId(config.getId());
        configForFrontend.setUrl(config.getUrl());
        configForFrontend.setKey("************************");

        return configForFrontend;
    }

    @Transactional
    public GeminiConfig updateOrCreateGeminiConfig(GeminiConfigDto geminiConfigDto) {
        UUID organizationId = JwtContextHolder.getOrganizationId();
        GeminiConfig config = geminiConfigRepository.findByOrganizationId(organizationId)
                .orElse(new GeminiConfig());

        config.setOrganizationId(organizationId);

        if (geminiConfigDto.getUrl() != null && !geminiConfigDto.getUrl().isBlank()) {
            config.setUrl(geminiConfigDto.getUrl());
        }
        if (geminiConfigDto.getKey() != null && !geminiConfigDto.getKey().isBlank()) {
            config.setKey(geminiConfigDto.getKey());
        }
        GeminiConfig saveGeminiConfig = geminiConfigRepository.save(config);

        GeminiConfig responseConfig = new GeminiConfig();
        responseConfig.setId(saveGeminiConfig.getId());
        responseConfig.setUrl(saveGeminiConfig.getUrl());
        responseConfig.setKey("************************");
        responseConfig.setOrganizationId(saveGeminiConfig.getOrganizationId());
        return responseConfig;
    }

}
