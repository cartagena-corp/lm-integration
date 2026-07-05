package com.cartagenacorp.lm_integration.Service.impl;

import com.cartagenacorp.lm_integration.Service.GeminiPromptService;
import com.cartagenacorp.lm_integration.Service.OrganizationExternalService;
import com.cartagenacorp.lm_integration.dto.GeminiPromptDTO;
import com.cartagenacorp.lm_integration.entity.GeminiPrompt;
import com.cartagenacorp.lm_integration.exceptions.BaseException;
import com.cartagenacorp.lm_integration.mapper.GeminiPromtMapper;
import com.cartagenacorp.lm_integration.repository.GeminiPromptRepository;
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GeminiPromptServiceImpl implements GeminiPromptService {

    private final GeminiPromptRepository geminiPromptRepository;
    private final GeminiPromtMapper geminiPromtMapper;
    private final OrganizationExternalService organizationExternalService;

    public GeminiPromptServiceImpl(GeminiPromptRepository geminiPromptRepository, GeminiPromtMapper geminiPromtMapper, OrganizationExternalService organizationExternalService) {
        this.geminiPromptRepository = geminiPromptRepository;
        this.geminiPromtMapper = geminiPromtMapper;
        this.organizationExternalService = organizationExternalService;
    }

    @Transactional
    public GeminiPromptDTO createGeminiPrompt(GeminiPromptDTO geminiPromptDTO){
        UUID organizationId = JwtContextHolder.getOrganizationId();

        String organizationName = organizationExternalService
                .getOrganizationName(JwtContextHolder.getToken(), organizationId)
                .orElseThrow(() -> new BaseException(
                        "No se encontró la organización",
                        HttpStatus.BAD_REQUEST.value()
                ));

        geminiPromptDTO.setId(null);
        geminiPromptDTO.setOrganizationId(organizationId);

        GeminiPrompt geminiPromptToSave = geminiPromtMapper.toEntity(geminiPromptDTO);
        GeminiPrompt savedGeminiPrompt = geminiPromptRepository.save(geminiPromptToSave);
        GeminiPromptDTO savedGeminiPromptDTO =  geminiPromtMapper.toDto(savedGeminiPrompt);

        return savedGeminiPromptDTO;
    }

    public List<GeminiPromptDTO> getAllGeminiPrompts(){
        UUID organizationId = JwtContextHolder.getOrganizationId();

        List<GeminiPrompt> geminiPrompts = geminiPromptRepository.findByOrganizationId(organizationId);
        List<GeminiPromptDTO> geminiPromptDTOs = geminiPromtMapper.toDto(geminiPrompts);

        return geminiPromptDTOs;
    }

    @Transactional
    public GeminiPromptDTO updateGeminiPrompt(GeminiPromptDTO geminiPromptDTO){
        UUID organizationId = JwtContextHolder.getOrganizationId();

        String organizationName = organizationExternalService
                .getOrganizationName(JwtContextHolder.getToken(), organizationId)
                .orElseThrow(() -> new BaseException(
                        "No se encontró la organización",
                        HttpStatus.BAD_REQUEST.value()
                ));

        GeminiPrompt existingGeminiPrompt = geminiPromptRepository.findById(geminiPromptDTO.getId())
                .orElseThrow(() -> new BaseException(
                        "No se encontró el prompt de Gemini",
                        HttpStatus.NOT_FOUND.value()
                ));

        geminiPromptDTO.setOrganizationId(organizationId);
        GeminiPrompt updatedGeminiPrompt = geminiPromtMapper.toEntity(geminiPromptDTO);
        GeminiPrompt savedGeminiPrompt = geminiPromptRepository.save(updatedGeminiPrompt);
        GeminiPromptDTO savedGeminiPromptDTO =  geminiPromtMapper.toDto(savedGeminiPrompt);

        return savedGeminiPromptDTO;
    }

    @Transactional
    public void deleteGeminiPromptById(UUID id){
        UUID organizationId = JwtContextHolder.getOrganizationId();

        GeminiPrompt existingGeminiPrompt = geminiPromptRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        "No se encontró el prompt de Gemini",
                        HttpStatus.NOT_FOUND.value()
                ));

        if (!existingGeminiPrompt.getOrganizationId().equals(organizationId)) {
            throw new BaseException(
                    "Error al eliminar el prompt de Gemini",
                    HttpStatus.FORBIDDEN.value()
            );
        }

        geminiPromptRepository.deleteById(id);
    }

}
