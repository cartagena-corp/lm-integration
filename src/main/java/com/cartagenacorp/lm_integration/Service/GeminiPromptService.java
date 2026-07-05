package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.GeminiPromptDTO;

import java.util.List;
import java.util.UUID;

public interface GeminiPromptService {
    GeminiPromptDTO createGeminiPrompt(GeminiPromptDTO geminiPromtDTO);

    List<GeminiPromptDTO> getAllGeminiPrompts();

    GeminiPromptDTO updateGeminiPrompt(GeminiPromptDTO geminiPromtDTO);

    void deleteGeminiPromptById(UUID id);
}
