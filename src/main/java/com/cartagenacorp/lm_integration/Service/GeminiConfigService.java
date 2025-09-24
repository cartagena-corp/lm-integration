package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.GeminiConfigDto;
import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import com.cartagenacorp.lm_integration.mapper.GeminiConfigMapper;
import com.cartagenacorp.lm_integration.repository.GeminiConfigRepository;
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GeminiConfigService {

    private final GeminiConfigRepository geminiConfigRepository;
    private final OrganizationExternalService organizationExternalService;
    private final GeminiConfigMapper geminiConfigMapper;

    public GeminiConfigService(GeminiConfigRepository geminiConfigRepository, OrganizationExternalService organizationExternalService, GeminiConfigMapper geminiConfigMapper) {
        this.geminiConfigRepository = geminiConfigRepository;
        this.organizationExternalService = organizationExternalService;
        this.geminiConfigMapper = geminiConfigMapper;
    }

    public GeminiConfig getGeminiConfigForInternalUse() {
        UUID organizationId = JwtContextHolder.getOrganizationId();
        GeminiConfig config = geminiConfigRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new RuntimeException("No se encontró configuración de Gemini para la organización " + organizationId));
        return config;
    }

    public GeminiConfigDto getGeminiConfigForFrontend() {
        UUID organizationId = JwtContextHolder.getOrganizationId();
        GeminiConfig config = geminiConfigRepository.findByOrganizationId(organizationId)
                .orElse(null);

        if (config == null) {
            return new GeminiConfigDto();
        }

        GeminiConfigDto configForFrontend = geminiConfigMapper.toDto(config);
        String organizationName = organizationExternalService.getOrganizationName(JwtContextHolder.getToken(), organizationId)
                .orElse("Desconocida");
        configForFrontend.setOrganizationName(organizationName);

        return configForFrontend;
    }

    @Transactional
    public GeminiConfigDto updateOrCreateGeminiConfig(GeminiConfigDto geminiConfigDto) {
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

        GeminiConfigDto configForFrontend = geminiConfigMapper.toDto(saveGeminiConfig);
        String organizationName = organizationExternalService.getOrganizationName(JwtContextHolder.getToken(), organizationId)
                .orElse("Desconocida");
        configForFrontend.setOrganizationName(organizationName);

        return configForFrontend;
    }

    private String obscureFirstHalf(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int length = value.length();
        int hidden = length / 2;
        String hiddenPart = "*".repeat(hidden);
        String visiblePart = value.substring(hidden);
        return hiddenPart + visiblePart;
    }

}
