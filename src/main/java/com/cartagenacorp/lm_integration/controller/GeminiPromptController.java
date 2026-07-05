package com.cartagenacorp.lm_integration.controller;

import com.cartagenacorp.lm_integration.Service.GeminiPromptService;
import com.cartagenacorp.lm_integration.dto.GeminiPromptDTO;
import com.cartagenacorp.lm_integration.dto.NotificationResponse;
import com.cartagenacorp.lm_integration.util.RequiresPermission;
import com.cartagenacorp.lm_integration.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gemini-prompts")
public class GeminiPromptController {

    private final GeminiPromptService geminiPromptService;

    public GeminiPromptController(GeminiPromptService geminiPromptService) {
        this.geminiPromptService = geminiPromptService;
    }

    @PostMapping()
    @RequiresPermission({"GEMINI_CONFIG"})
    public ResponseEntity<GeminiPromptDTO> createGeminiPromt(@RequestBody GeminiPromptDTO geminiPromptDTO) {
        GeminiPromptDTO geminiPromt = geminiPromptService.createGeminiPrompt(geminiPromptDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(geminiPromt);
    }

    @GetMapping()
    @RequiresPermission({"GEMINI_CONFIG", "GEMINI_ACTIVE"})
    public ResponseEntity<List<GeminiPromptDTO>> getAllGeminiPrompts() {
        List<GeminiPromptDTO> geminiPrompts = geminiPromptService.getAllGeminiPrompts();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(geminiPrompts);
    }

    @PutMapping()
    @RequiresPermission({"GEMINI_CONFIG"})
    public ResponseEntity<GeminiPromptDTO> updateGeminiPromt(@RequestBody GeminiPromptDTO geminiPromptDTO) {
        GeminiPromptDTO geminiPromt = geminiPromptService.updateGeminiPrompt(geminiPromptDTO);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(geminiPromt);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission({"GEMINI_CONFIG"})
    public ResponseEntity<NotificationResponse> deleteGeminiPromtById(@PathVariable("id") UUID id) {
        geminiPromptService.deleteGeminiPromptById(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseUtil.success("Gemini prompt deleted successfully."));
    }
}
