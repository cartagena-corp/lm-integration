package com.cartagenacorp.lm_integration.controller;

import com.cartagenacorp.lm_integration.Service.GeminiService;
import com.cartagenacorp.lm_integration.dto.IssueDTOGemini;
import com.cartagenacorp.lm_integration.util.RequiresPermission;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping(value = "/detectIssuesFromText/{projectId}") // ya esta
    @RequiresPermission({"IMPORT_PROJECT"})
    public ResponseEntity<List<IssueDTOGemini>> detectIssuesWithGeminiText(
            @PathVariable("projectId") String projectId,
            @RequestBody String texto
    ) {
        List<IssueDTOGemini> resultado = geminiService.detectIssues(projectId, texto);
        return ResponseEntity.status(HttpStatus.OK).body(resultado);
    }

//    @PostMapping(value = "/detectIssuesFromFile/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @RequiresPermission({"IMPORT_PROJECT"})
//    public ResponseEntity<?> detectIssuesWithGeminiFile(
//            @RequestPart("projectId") String projectId,
//            @RequestPart("file") MultipartFile file
//    ) {
//        return geminiService.createIssues(projectId, file);
//    }
}
