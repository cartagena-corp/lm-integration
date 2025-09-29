package com.cartagenacorp.lm_integration.controller;

import com.cartagenacorp.lm_integration.Service.ProjectImportService;
import com.cartagenacorp.lm_integration.util.RequiresPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class ProjectImportController {

    private final ProjectImportService projectImportService;

    @Autowired
    public ProjectImportController(ProjectImportService projectImportService) {
        this.projectImportService = projectImportService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission({"IMPORT_PROJECT"})
    public ResponseEntity<?> importProjectWithIssues(
            @RequestPart("projectId") String projectId,
            @RequestPart(value = "sprintId", required = false) String sprintId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("mapping") String mappingJson
    ) {
        return projectImportService.importProjectWithIssues(projectId, sprintId, file, mappingJson);
    }

    @PostMapping("/columns")
    @RequiresPermission({"IMPORT_PROJECT"})
    public ResponseEntity<?> getExcelColumns(@RequestParam("file") MultipartFile file) {
        return projectImportService.extractExcelColumns(file);
    }
}