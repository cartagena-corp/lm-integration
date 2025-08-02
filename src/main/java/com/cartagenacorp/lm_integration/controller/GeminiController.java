package com.cartagenacorp.lm_integration.controller;

import com.cartagenacorp.lm_integration.Service.GeminiService;
import com.cartagenacorp.lm_integration.dto.GeminiResponseDTO;
import com.cartagenacorp.lm_integration.util.RequiresPermission;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping(value = "/detectIssues", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission({"GEMINI_ACTIVE"})
    public ResponseEntity<?> detectIssuesMixed(
            @RequestPart("projectId") String projectId,
            @RequestPart(value = "texto", required = false) String texto,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {

        GeminiResponseDTO resultado = geminiService.detectIssuesFromTextAndOrDocx(projectId, texto, file);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission({"GEMINI_ACTIVE"})
    public ResponseEntity<String> chat(
            @RequestPart("texto") String texto,
            @RequestPart(value = "archivos", required = false) MultipartFile[] archivos
    ) {
        String response = geminiService.chat(texto, archivos);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
