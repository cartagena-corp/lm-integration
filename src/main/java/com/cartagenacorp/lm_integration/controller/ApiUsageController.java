package com.cartagenacorp.lm_integration.controller;

import com.cartagenacorp.lm_integration.Service.ApiUsageService;
import com.cartagenacorp.lm_integration.dto.ApiUsageFiltersDto;
import com.cartagenacorp.lm_integration.dto.ApiUsageLogDto;
import com.cartagenacorp.lm_integration.dto.PageResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/gemini-usage")
public class ApiUsageController {

    private final ApiUsageService apiUsageService;

    public ApiUsageController(ApiUsageService apiUsageService) {
        this.apiUsageService = apiUsageService;
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiUsageFiltersDto> getFilters() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(apiUsageService.getUniqueFilters());
    }

    @GetMapping("/logs")
    public PageResponseDTO<ApiUsageLogDto> getLogs(
            @RequestParam(required = false) String feature,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return apiUsageService.getLogs(feature, projectId, userEmail, page, size);
    }

}
