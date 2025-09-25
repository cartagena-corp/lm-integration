package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.DescriptionDTO;
import com.cartagenacorp.lm_integration.dto.IssueDTO;
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ProjectImportService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectImportService.class);

    @Value("${project.service.url}")
    private String projectServiceUrl;

    @Value("${issue.service.url}")
    private String issueServiceUrl;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    private final RestTemplate restTemplate;

    public ProjectImportService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<?> importProjectWithIssues(String projectId, MultipartFile file, String mappingJson) {
        logger.info("=== [ProjectImportService] Iniciando importación de issues para el proyecto con ID={} ===", projectId);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> mapping = objectMapper.readValue(mappingJson, Map.class);

            logger.debug("[ProjectImportService] Mapeo recibido: {}", mapping);

            String token = JwtContextHolder.getToken();
            List<IssueDTO> issuesToSend = extractIssuesFromExcel(file, UUID.fromString(projectId), mapping, token);

            logger.info("[ProjectImportService] Total de issues preparados para envío: {} al servicio lm-issues", issuesToSend.size());

            restTemplate.exchange(
                    issueServiceUrl + "/batch",
                    HttpMethod.POST,
                    buildHttpEntity(issuesToSend, token),
                    Void.class
            );

            logger.info("=== [ProjectImportService] Importación finalizada correctamente para el proyecto con ID={} ===", projectId);
            return ResponseEntity.ok("Successful import");

        } catch (Exception e) {
            logger.error("[ProjectImportService] Error al importar issues para el proyecto con ID={}: {}", projectId, e.getMessage(), e);
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", "ISSUE_CREATION_FAILED");
            errorBody.put("message", "An error occurred while adding issues to the project.");
            errorBody.put("details", List.of(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    private List<IssueDTO> extractIssuesFromExcel(MultipartFile file, UUID projectId, Map<String, String> mapping, String token) throws Exception {
        logger.info("[ProjectImportService] Extrayendo issues desde Excel para el proyecto con ID={}", projectId);
        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);

        List<IssueDTO> issuesToSend = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;

            String title = getCellValueByMapping(row, headerRow, mapping.get("title"));

            if (title == null || title.isBlank()) {
                logger.warn("[ProjectImportService] Fila {} ignorada: no contiene título", row.getRowNum() + 1);
                continue;
            }

            List<DescriptionDTO> descriptions = new ArrayList<>();
            if (mapping.containsKey("descriptions")) {
                String[] descColumns = mapping.get("descriptions").split(",");
                for (String descCol : descColumns) {
                    String content = getCellValueByMapping(row, headerRow, descCol.trim());
                    if (content != null && content.length() > MAX_DESCRIPTION_LENGTH) {
                        logger.error("[ProjectImportService] Descripción demasiado larga en fila {}, columna '{}'", row.getRowNum() + 1, descCol);

                        throw new IllegalArgumentException("The description in the row " + (row.getRowNum() + 1) +
                                " and column '" + descCol + "' exceeds the maximum of " + MAX_DESCRIPTION_LENGTH + " characters");
                    }
                    if (content != null && !content.isEmpty()) {
                        descriptions.add(new DescriptionDTO(descCol, content));
                    }
                }
            }

            UUID assignedId = null;

            if (mapping.containsKey("assignedId")) {
                String assignedRaw = getCellValueByMapping(row, headerRow, mapping.get("assignedId"));
                if (assignedRaw != null && !assignedRaw.isBlank()) {
                    assignedId = resolveUserIdByIdentifier(assignedRaw, token);
                    if (assignedId == null) {
                        logger.warn("[ProjectImportService] No se pudo resolver usuario '{}' en fila {}", assignedRaw, row.getRowNum() + 1);
                    }
                }
            }

            issuesToSend.add(new IssueDTO(title.trim(), descriptions, 0, projectId, null, null, null, null, assignedId));
            logger.debug("[ProjectImportService] Issue agregado desde fila {}: title='{}', descriptions={}, assignedId={}", row.getRowNum() + 1, title.trim(), descriptions.size(), assignedId);
        }
        logger.info("[ProjectImportService] Total de issues extraídos: {}", issuesToSend.size());
        return issuesToSend;
    }

    private String getCellValueByMapping(Row row, Row headerRow, String columnName) {
        if (columnName == null) return null;
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell headerCell = headerRow.getCell(i);
            if (headerCell != null && columnName.equalsIgnoreCase(headerCell.getStringCellValue().trim())) {
                Cell valueCell = row.getCell(i);
                return (valueCell != null) ? valueCell.toString().trim() : null;
            }
        }
        return null;
    }

    private <T> HttpEntity<T> buildHttpEntity(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private UUID resolveUserIdByIdentifier(String identifier, String token) {
        try {
            logger.debug("[ProjectImportService] Resolviendo usuario con identificador '{}' en servicio lm-oauth", identifier);
            String url = authServiceUrl +"/users/resolve?identifier=" + URLEncoder.encode(identifier, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            ResponseEntity<UUID> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    UUID.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("[ProjectImportService] Usuario resuelto exitosamente: {}", response.getBody());
            } else {
                logger.warn("[ProjectImportService] No se pudo resolver usuario '{}'. Status={}", identifier, response.getStatusCode());
            }

            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (Exception e) {
            logger.error("[ProjectImportService] Error al resolver usuario '{}': {}", identifier, e.getMessage(), e);
            return null;
        }
    }

    public ResponseEntity<?> extractExcelColumns(MultipartFile file) {
        logger.info("=== [ProjectImportService] Extrayendo columnas desde archivo Excel ===");
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                logger.warn("[ProjectImportService] El archivo no contiene encabezados en la primera fila");
                return ResponseEntity.badRequest().body("El archivo no tiene encabezados en la primera fila.");
            }

            List<String> columns = new ArrayList<>();
            for (Cell cell : headerRow) {
                if (cell != null) {
                    String value = cell.getStringCellValue().trim();
                    if (!value.isEmpty()) {
                        columns.add(value);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("columns", columns);
            response.put("sampleRow", getSampleRow(sheet));

            logger.info("[ProjectImportService] Columnas extraídas correctamente: {}", columns);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("[ProjectImportService] Error al leer columnas del Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "READ_EXCEL_ERROR", "message", e.getMessage()));
        }
    }

    private Map<String, Object> getSampleRow(Sheet sheet) {
        Map<String, Object> sampleRow = new LinkedHashMap<>();
        Row headerRow = sheet.getRow(0);
        Row secondRow = sheet.getRow(1);

        if (secondRow != null) {
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell headerCell = headerRow.getCell(i);
                Cell valueCell = secondRow.getCell(i);
                String header = headerCell != null ? headerCell.getStringCellValue() : "Column " + (i + 1);
                String value = valueCell != null ? valueCell.toString() : "";
                sampleRow.put(header, value);
            }
        }
        logger.debug("[ProjectImportService] Fila de ejemplo obtenida: {}", sampleRow);
        return sampleRow;
    }
}

