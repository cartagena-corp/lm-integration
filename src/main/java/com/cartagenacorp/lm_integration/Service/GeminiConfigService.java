package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.GeminiConfigDto;
import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import com.cartagenacorp.lm_integration.repository.GeminiConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeminiConfigService {

    private final GeminiConfigRepository geminiConfigRepository;

    public GeminiConfigService(GeminiConfigRepository geminiConfigRepository) {
        this.geminiConfigRepository = geminiConfigRepository;
    }

    public GeminiConfig getGeminiConfigForInternalUse() {
        GeminiConfig config = geminiConfigRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró la configuración de Gemini en la base de datos. Asegúrate de que exista un registro."));
        return config;
    }

    public GeminiConfig getGeminiConfigForFrontend() {
        GeminiConfig config = geminiConfigRepository.findAll().stream()
                .findFirst()
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
        GeminiConfig config = geminiConfigRepository.findAll().stream()
                .findFirst()
                .orElse(new GeminiConfig());

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
        return responseConfig;
    }

}
