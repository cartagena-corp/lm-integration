package com.cartagenacorp.lm_integration.controller;

import com.cartagenacorp.lm_integration.Service.GeminiConfigService;
import com.cartagenacorp.lm_integration.dto.GeminiConfigDto;
import com.cartagenacorp.lm_integration.util.RequiresPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gemini-config")
public class GeminiConfigController {

    private final GeminiConfigService geminiConfigService;

    public GeminiConfigController(GeminiConfigService geminiConfigService) {
        this.geminiConfigService = geminiConfigService;
    }

    @GetMapping
    @RequiresPermission({"GEMINI_CONFIG"})
    public ResponseEntity<GeminiConfigDto> getGeminiConfig() {
        GeminiConfigDto config = geminiConfigService.getGeminiConfigForFrontend();
        return ResponseEntity.ok(config);
    }

    @PutMapping
    @RequiresPermission({"GEMINI_CONFIG"})
    public ResponseEntity<GeminiConfigDto> updateOrCreateGeminiConfig(@RequestBody GeminiConfigDto geminiConfigDto) {
        GeminiConfigDto updatedConfig = geminiConfigService.updateOrCreateGeminiConfig(geminiConfigDto);
        return ResponseEntity.ok(updatedConfig);
    }

}
