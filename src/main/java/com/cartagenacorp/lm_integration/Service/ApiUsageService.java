package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.ApiUsageFiltersDto;
import com.cartagenacorp.lm_integration.dto.ApiUsageLogDto;
import com.cartagenacorp.lm_integration.dto.PageResponseDTO;
import com.cartagenacorp.lm_integration.dto.ProjectFilterDto;
import com.cartagenacorp.lm_integration.entity.ApiUsageLog;
import com.cartagenacorp.lm_integration.mapper.ApiUsageLogMapper;
import com.cartagenacorp.lm_integration.repository.ApiUsageLogRepository;
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ApiUsageService {

    private static final Logger logger = LoggerFactory.getLogger(ApiUsageService.class);

    private final ApiUsageLogRepository apiUsageLogRepository;
    private final ApiUsageLogMapper apiUsageLogMapper;

    public ApiUsageService(ApiUsageLogRepository apiUsageLogRepository, ApiUsageLogMapper apiUsageLogMapper) {
        this.apiUsageLogRepository = apiUsageLogRepository;
        this.apiUsageLogMapper = apiUsageLogMapper;
    }

    public ApiUsageFiltersDto getUniqueFilters() {
        logger.info("=== [ApiUsageService] Iniciando flujo de obtención de filtros únicos de logs de uso de API ===");

        UUID organizationId = JwtContextHolder.getOrganizationId();
        logger.debug("[ApiUsageService] Consultando filtros para organization con ID={}", organizationId);

        List<String> features = apiUsageLogRepository.findDistinctFeatures(organizationId);
        List<ProjectFilterDto> projects = apiUsageLogRepository.findProjectsUsedInApiLogs(organizationId);
        List<String> emails = apiUsageLogRepository.findDistinctUserEmails(organizationId);
        logger.info("[ApiUsageService] Filtros obtenidos - features={}, projects={}, emails={}", features.size(), projects.size(), emails.size());

        logger.info("=== [ApiUsageService] Finalizando flujo de obtención de filtros únicos correctamente ===");
        return new ApiUsageFiltersDto(features, projects, emails);
    }

    public PageResponseDTO<ApiUsageLogDto> getLogs(String feature, UUID projectId, String userEmail, int page, int size) {
        logger.info("=== [ApiUsageService] Iniciando flujo de obtención de logs de uso de API ===");
        logger.debug("[ApiUsageService] Parámetros de búsqueda -> feature={}, projectId={}, userEmail={}, page={}, size={}", feature, projectId, userEmail, page, size);

        UUID organizationId = JwtContextHolder.getOrganizationId();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<ApiUsageLog> result = apiUsageLogRepository.findByFilters(organizationId, feature, projectId, userEmail, pageable);

        logger.info("[ApiUsageService] Total de registros encontrados={}", result.getTotalElements());
        Page<ApiUsageLogDto> dtoPage = result.map(log -> apiUsageLogMapper.toDto(log));

        logger.info("=== [ApiUsageService] Finalizando flujo de obtención de logs correctamente con {} registros en la página ===", dtoPage.getContent().size());
        return new PageResponseDTO<>(dtoPage);
    }
}
