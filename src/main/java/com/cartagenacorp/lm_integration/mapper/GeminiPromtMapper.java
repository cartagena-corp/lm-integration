package com.cartagenacorp.lm_integration.mapper;

import com.cartagenacorp.lm_integration.dto.GeminiPromptDTO;
import com.cartagenacorp.lm_integration.entity.GeminiPrompt;
import org.mapstruct.*;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface GeminiPromtMapper {
    GeminiPrompt toEntity(GeminiPromptDTO geminiPromtDTO);

    GeminiPromptDTO toDto(GeminiPrompt geminiPromt);

    List<GeminiPrompt> toEntity(List<GeminiPromptDTO> geminiPromtDTO);

    List<GeminiPromptDTO> toDto(List<GeminiPrompt> geminiPromt);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    GeminiPrompt partialUpdate(GeminiPromptDTO geminiPromtDTO, @MappingTarget GeminiPrompt geminiPromt);
}