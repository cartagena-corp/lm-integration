package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.ApiUsageFiltersDto;
import com.cartagenacorp.lm_integration.dto.ApiUsageLogDto;
import com.cartagenacorp.lm_integration.dto.PageResponseDTO;
import com.cartagenacorp.lm_integration.dto.ProjectFilterDto;
import com.cartagenacorp.lm_integration.entity.ApiUsageLog;
import com.cartagenacorp.lm_integration.mapper.ApiUsageLogMapper;
import com.cartagenacorp.lm_integration.repository.ApiUsageLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ApiUsageService {

    private final ApiUsageLogRepository apiUsageLogRepository;
    private final ApiUsageLogMapper apiUsageLogMapper;

    public ApiUsageService(ApiUsageLogRepository apiUsageLogRepository, ApiUsageLogMapper apiUsageLogMapper) {
        this.apiUsageLogRepository = apiUsageLogRepository;
        this.apiUsageLogMapper = apiUsageLogMapper;
    }

    public ApiUsageFiltersDto getUniqueFilters() {
        List<String> features = apiUsageLogRepository.findDistinctFeatures();
        List<ProjectFilterDto> projects = apiUsageLogRepository.findProjectsUsedInApiLogs();
        List<String> emails = apiUsageLogRepository.findDistinctUserEmails();

        return new ApiUsageFiltersDto(features, projects, emails);
    }

    public PageResponseDTO<ApiUsageLogDto> getLogs(String feature, UUID projectId, String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<ApiUsageLog> result = apiUsageLogRepository.findByFilters(feature, projectId, userEmail, pageable);

        Page<ApiUsageLogDto> dtoPage = result.map(log -> apiUsageLogMapper.toDto(log));

        return new PageResponseDTO<>(dtoPage);
    }
}
