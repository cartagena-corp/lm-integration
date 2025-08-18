package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.DescriptionDTO;
import com.cartagenacorp.lm_integration.dto.IssueDTO;git
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${project.service.url}")
    private String projectServiceUrl;

    @Value("${issue.service.url}")
    private String issueServiceUrl;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    private final RestTemplate restTemplate;

    @Autowired
    public ProjectImportService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<?> importProjectWithIssues(String projectId, MultipartFile file, String mappingJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> mapping = objectMapper.readValue(mappingJson, Map.class);

            String token = JwtContextHolder.getToken();

            List<IssueDTO> issuesToSend = extractIssuesFromExcel(file, UUID.fromString(projectId), mapping, token);

            restTemplate.exchange(
                    issueServiceUrl + "/batch",
                    HttpMethod.POST,
                    buildHttpEntity(issuesToSend, token),
                    Void.class
            );

            return ResponseEntity.ok("Successful import");

        } catch (Exception e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", "ISSUE_CREATION_FAILED");
            errorBody.put("message", "An error occurred while adding issues to the project.");
            errorBody.put("details", List.of(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    private List<IssueDTO> extractIssuesFromExcel(MultipartFile file, UUID projectId, Map<String, String> mapping, String token) throws Exception {
        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);

        List<IssueDTO> issuesToSend = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;

            String title = getCellValueByMapping(row, headerRow, mapping.get("title"));

            if (title == null || title.isBlank()) {
                continue;
            }

            List<DescriptionDTO> descriptions = new ArrayList<>();
            if (mapping.containsKey("descriptions")) {
                String[] descColumns = mapping.get("descriptions").split(",");
                for (String descCol : descColumns) {
                    String content = getCellValueByMapping(row, headerRow, descCol.trim());
                    if (content != null && content.length() > MAX_DESCRIPTION_LENGTH) {
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
                }
            }

            issuesToSend.add(new IssueDTO(title.trim(), descriptions, 0, projectId, null, null, null, null, assignedId));
        }
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
            String url = authServiceUrl +"/users/resolve?identifier=" + URLEncoder.encode(identifier, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            ResponseEntity<UUID> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    UUID.class
            );

            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public ResponseEntity<?> extractExcelColumns(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                return ResponseEntity.badRequest().body("El archivo no tiene encabezados en la primera fila.");
            }

            List<String> columns = new ArrayList<>();
            for (Cell cell : headerRow) {
                columns.add(cell.getStringCellValue().trim());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("columns", columns);
            response.put("sampleRow", getSampleRow(sheet));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
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
        return sampleRow;
    }
}

