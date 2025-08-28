package com.cartagenacorp.lm_integration.repository;

import com.cartagenacorp.lm_integration.dto.ProjectFilterDto;
import com.cartagenacorp.lm_integration.entity.ApiUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiUsageLogRepository extends JpaRepository<ApiUsageLog, Long> {

    @Query("SELECT DISTINCT a.feature FROM ApiUsageLog a WHERE a.organizationId = :organizationId")
    List<String> findDistinctFeatures(@Param("organizationId") UUID organizationId);

    @Query(value = """
        SELECT p.id AS id, p.name AS name
        FROM project p
        WHERE p.id IN (
            SELECT DISTINCT a.project_id
            FROM api_usage_log a
            WHERE a.organization_id = :organizationId
              AND a.project_id IS NOT NULL
        )
    """, nativeQuery = true)
    List<ProjectFilterDto> findProjectsUsedInApiLogs(@Param("organizationId") UUID organizationId);

    @Query("SELECT DISTINCT a.userEmail FROM ApiUsageLog a WHERE a.organizationId = :organizationId")
    List<String> findDistinctUserEmails(@Param("organizationId") UUID organizationId);

    @Query("""
        SELECT a
        FROM ApiUsageLog a
        WHERE a.organizationId = :organizationId
          AND ((:feature IS NULL OR :feature = '') OR a.feature = :feature)
          AND ((:projectId IS NULL) OR a.projectId = :projectId)
          AND ((:userEmail IS NULL OR :userEmail = '') OR a.userEmail = :userEmail)
    """)
    Page<ApiUsageLog> findByFilters(
            @Param("organizationId") UUID organizationId,
            @Param("feature") String feature,
            @Param("projectId") UUID projectId,
            @Param("userEmail") String userEmail,
            Pageable pageable
    );

}