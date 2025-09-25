package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.GeminiConfigDto;
import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import com.cartagenacorp.lm_integration.mapper.GeminiConfigMapper;
import com.cartagenacorp.lm_integration.repository.GeminiConfigRepository;
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GeminiConfigService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiConfigService.class);

    private final GeminiConfigRepository geminiConfigRepository;
    private final OrganizationExternalService organizationExternalService;
    private final GeminiConfigMapper geminiConfigMapper;

    public GeminiConfigService(GeminiConfigRepository geminiConfigRepository, OrganizationExternalService organizationExternalService, GeminiConfigMapper geminiConfigMapper) {
        this.geminiConfigRepository = geminiConfigRepository;
        this.organizationExternalService = organizationExternalService;
        this.geminiConfigMapper = geminiConfigMapper;
    }

    public GeminiConfig getGeminiConfigForInternalUse() {
        logger.info("=== [GeminiConfigService] Iniciando flujo de obtención de GeminiConfig para uso interno ===");

        UUID organizationId = JwtContextHolder.getOrganizationId();
        logger.info("[GeminiConfigService] Consultando en DB la configuración de Gemini para la organización con ID={}", organizationId);

        GeminiConfig config = geminiConfigRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> {
                    logger.error("[GeminiConfigService] No se encontró configuración de Gemini para la organización con ID={}", organizationId);
                    return new RuntimeException("No se encontró configuración de Gemini para la organización " + organizationId);
                });

        logger.info("=== [GeminiConfigService] Finalizando flujo de obtención de GeminiConfig para uso interno correctamente ===");
        return config;
    }

    public GeminiConfigDto getGeminiConfigForFrontend() {
        logger.info("=== [GeminiConfigService] Iniciando flujo de obtención de GeminiConfig para el front ===");

        UUID organizationId = JwtContextHolder.getOrganizationId();
        logger.info("[GeminiConfigService] Consultando en DB la configuración de Gemini para la organización con ID={}", organizationId);

        GeminiConfig config = geminiConfigRepository.findByOrganizationId(organizationId)
                .orElse(null);

        if (config == null) {
            logger.warn("[GeminiConfigService] No se encontró configuración de Gemini para la organización con ID={}. Se devolverá un DTO vacío.", organizationId);
            return new GeminiConfigDto();
        }

        GeminiConfigDto configForFrontend = geminiConfigMapper.toDto(config);
        String organizationName = organizationExternalService.getOrganizationName(JwtContextHolder.getToken(), organizationId)
                .orElseGet(() -> {
                    logger.warn("[GeminiConfigService] No se pudo obtener el nombre de la organización con ID={}. Se usará 'Desconocida'.", organizationId);
                    return "Desconocida";
                });
        configForFrontend.setOrganizationName(organizationName);

        logger.info("=== [GeminiConfigService] Finalizando flujo de obtención de GeminiConfig para el front correctamente ===");
        return configForFrontend;
    }

    @Transactional
    public GeminiConfigDto updateOrCreateGeminiConfig(GeminiConfigDto geminiConfigDto) {
        logger.info("=== [GeminiConfigService] Iniciando flujo de creación/actualización de GeminiConfig ===");

        UUID organizationId = JwtContextHolder.getOrganizationId();
        logger.info("[GeminiConfigService] Consultando en DB la configuración de Gemini para la organización con ID={}", organizationId);

        GeminiConfig config = geminiConfigRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    logger.info("[GeminiConfigService] No existe configuración previa para la organización con ID={}. Se creará una nueva.", organizationId);
                    return new GeminiConfig();
                });

        config.setOrganizationId(organizationId);

        if (geminiConfigDto.getUrl() != null && !geminiConfigDto.getUrl().isBlank()) {
            logger.debug("[GeminiConfigService] Actualizando URL de GeminiConfig");
            config.setUrl(geminiConfigDto.getUrl());
        }
        if (geminiConfigDto.getKey() != null && !geminiConfigDto.getKey().isBlank()) {
            logger.debug("[GeminiConfigService] Actualizando Key de GeminiConfig");
            config.setKey(geminiConfigDto.getKey());
        }
        GeminiConfig saveGeminiConfig = geminiConfigRepository.save(config);

        GeminiConfigDto configForFrontend = geminiConfigMapper.toDto(saveGeminiConfig);
        String organizationName = organizationExternalService.getOrganizationName(JwtContextHolder.getToken(), organizationId)
                .orElseGet(() -> {
                    logger.warn("[GeminiConfigService] No se pudo obtener el nombre de la organización con ID={}. Se usará 'Desconocida'.", organizationId);
                    return "Desconocida";
                });
        configForFrontend.setOrganizationName(organizationName);

        logger.info("=== [GeminiConfigService] Finalizando flujo de creación/actualización de GeminiConfig correctamente ===");
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
