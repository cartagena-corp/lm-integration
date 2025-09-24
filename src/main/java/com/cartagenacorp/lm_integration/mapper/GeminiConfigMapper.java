package com.cartagenacorp.lm_integration.mapper;

import com.cartagenacorp.lm_integration.dto.GeminiConfigDto;
import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface GeminiConfigMapper {
    GeminiConfig toEntity(GeminiConfigDto geminiConfigDto);

    GeminiConfigDto toDto(GeminiConfig geminiConfig);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    GeminiConfig partialUpdate(GeminiConfigDto geminiConfigDto, @MappingTarget GeminiConfig geminiConfig);
}