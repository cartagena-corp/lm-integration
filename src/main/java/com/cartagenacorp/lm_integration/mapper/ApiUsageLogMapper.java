package com.cartagenacorp.lm_integration.mapper;

import com.cartagenacorp.lm_integration.dto.ApiUsageLogDto;
import com.cartagenacorp.lm_integration.entity.ApiUsageLog;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ApiUsageLogMapper {
    ApiUsageLog toEntity(ApiUsageLogDto apiUsageLogDto);

    ApiUsageLogDto toDto(ApiUsageLog apiUsageLog);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ApiUsageLog partialUpdate(ApiUsageLogDto apiUsageLogDto, @MappingTarget ApiUsageLog apiUsageLog);
}