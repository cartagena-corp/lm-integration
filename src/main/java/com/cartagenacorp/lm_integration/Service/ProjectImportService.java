package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.DescriptionDTO;
import com.cartagenacorp.lm_integration.dto.IssueDTO;
import com.cartagenacorp.lm_integration.dto.ProjectDTO;
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    private static final int MAX_DESCRIPTION_LENGTH = 1500;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProjectImportService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<?> importProjectWithIssues(String projectJson, MultipartFile file) {
        UUID projectId = null;
        try {
            String token = JwtContextHolder.getToken();

            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            ProjectDTO projectDTO = objectMapper.readValue(projectJson, ProjectDTO.class);

            ResponseEntity<ProjectDTO> projectResponse = restTemplate.exchange(
                    projectServiceUrl,
                    HttpMethod.POST,
                    buildHttpEntity(projectDTO, token),
                    ProjectDTO.class);

            if (!projectResponse.getStatusCode().is2xxSuccessful() || projectResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating project");
            }

            projectId = projectResponse.getBody().getId();

            List<IssueDTO> issuesToSend = extractIssuesFromExcel(file, projectId);

            restTemplate.exchange(issueServiceUrl + "/batch", HttpMethod.POST,
                    buildHttpEntity(issuesToSend, token), Void.class);

            return ResponseEntity.ok("Successful import");
        } catch (Exception e) {
            if (projectId != null) {
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("error", "ISSUE_CREATION_FAILED");
                errorBody.put("message", "An error occurred while adding issues to the project. Please delete the project and try again");
                errorBody.put("details", List.of(e.getMessage()));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    private List<IssueDTO> extractIssuesFromExcel(MultipartFile file, UUID projectId) throws Exception {
        String token = JwtContextHolder.getToken();

        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        List<IssueDTO> issuesToSend = new ArrayList<>();
        Row headerRow = sheet.getRow(0);

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;

            String title = row.getCell(0).getStringCellValue();
            List<DescriptionDTO> descriptions = new ArrayList<>();

            for (int i = 1; i <= 3; i++) {
                if (row.getCell(i) != null && headerRow.getCell(i) != null) {
                    String columnTitle = headerRow.getCell(i).getStringCellValue();
                    String content = row.getCell(i).getStringCellValue();
                    String fullDescription = "**" + columnTitle + "**\n" + content;

                    if (fullDescription.length() > MAX_DESCRIPTION_LENGTH) {
                        throw new IllegalArgumentException("The description in the row " + (row.getRowNum() + 1) +
                                " and column '" + columnTitle + "' exceeds the maximum of " + MAX_DESCRIPTION_LENGTH + " characters");
                    }

                    descriptions.add(new DescriptionDTO(fullDescription));
                }
            }

            Cell assignedCell = row.getCell(5);
            UUID assignedId = null;

            if (assignedCell != null && assignedCell.getCellType() != CellType.BLANK) {
                String assignedRaw = assignedCell.getStringCellValue().trim();
                if (!assignedRaw.isEmpty()) {
                    assignedId = resolveUserIdByIdentifier(assignedRaw, token);
                    if (assignedId == null) {
                        throw new IllegalArgumentException("No user found for the identifier '" + assignedRaw + "' in line " + (row.getRowNum() + 1));
                    }
                }
            }

            issuesToSend.add(new IssueDTO(title, descriptions, 0, projectId, null, null, null, null, assignedId));
        }
        return issuesToSend;
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
}

