package com.cartagenacorp.lm_integration.controller;

import com.cartagenacorp.lm_integration.Service.GeminiService;
import com.cartagenacorp.lm_integration.dto.IssueDTOGemini;
import com.cartagenacorp.lm_integration.util.RequiresPermission;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping(value = "/detectIssuesFromText/{projectId}")
    @RequiresPermission({"GEMINI_ACTIVE"})
    public ResponseEntity<List<IssueDTOGemini>> detectIssuesWithGeminiText(
            @PathVariable("projectId") String projectId,
            @RequestBody String texto
    ) {
        List<IssueDTOGemini> resultado = geminiService.detectIssues(projectId, texto);
        return ResponseEntity.status(HttpStatus.OK).body(resultado);
    }

    @PostMapping(value = "/detectIssuesFromDocx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission({"GEMINI_ACTIVE"})
    public ResponseEntity<?> detectIssuesWithGeminiFromDocx(
            @RequestPart("projectId") String projectId,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            List<IssueDTOGemini> issues = geminiService.detectIssuesFromDocx(projectId, file.getInputStream());
            return ResponseEntity.ok(issues);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/chat")
    @RequiresPermission({"GEMINI_ACTIVE"})
    public ResponseEntity<String> chat(@RequestBody String texto) {
        String response = geminiService.chat(texto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
