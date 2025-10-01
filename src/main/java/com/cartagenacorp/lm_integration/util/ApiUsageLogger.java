package com.cartagenacorp.lm_integration.util;

import com.cartagenacorp.lm_integration.entity.ApiUsageLog;
import com.cartagenacorp.lm_integration.repository.ApiUsageLogRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ApiUsageLogger {

    private final ApiUsageLogRepository apiUsageLogRepository;

    public ApiUsageLogger(ApiUsageLogRepository apiUsageLogRepository) {
        this.apiUsageLogRepository = apiUsageLogRepository;
    }

    public void log(String feature, String projectId, long startTime, String status) {
        long responseTimeMs = System.currentTimeMillis() - startTime;

        UUID userId = JwtContextHolder.getUserId();
        String userEmail = JwtContextHolder.getUserEmail();

        ApiUsageLog log = ApiUsageLog.builder()
                .feature(feature)
                .projectId(projectId != null ? UUID.fromString(projectId) : null)
                .userId(userId)
                .userEmail(userEmail)
                .timestamp(LocalDateTime.now())
                .responseTimeMs(responseTimeMs)
                .status(status)
                .organizationId(JwtContextHolder.getOrganizationId())
                .build();

        apiUsageLogRepository.save(log);
    }
}
