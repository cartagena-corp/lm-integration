package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.IssueDTOGemini;
import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private final GeminiConfigService geminiConfigService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiService(GeminiConfigService geminiConfigService) {
        this.geminiConfigService = geminiConfigService;
    }

    public List<IssueDTOGemini> detectIssues(String projectId, String texto) {
        GeminiConfig geminiConfig = geminiConfigService.getGeminiConfigForInternalUse();

        String prompt = String.format("""
        Analiza el siguiente texto y devuelve una lista de tareas en formato JSON como este:
        No me devuelvas ninguna otra respuesta aparte del ```json y el ``` para encerrar el array de issues
        [
            {
                "title": "CREAR CRUD DE USUARIOS",
                "descriptionsDTO": [
                    { "title": "Requerimientos", "text": "El sistema debe permitir crear, leer, actualizar y eliminar usuarios" },
                    { "title": "Validaciones", "text": "El correo debe ser único" }
                ],
                "projectId": Asegúrate de que cada objeto en la lista tenga en este campo "projectId" el valor "%s".,
                "assignedId": equipo, persona mencionada, no se deja claro
            }
        ]
        

        Texto: %s
        """, projectId, texto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = geminiConfig.getUrl() + "?key=" + geminiConfig.getKey();

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();

            String rawText = mapper.readTree(response.getBody())
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            String respuestaJson = rawText
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            List<IssueDTOGemini> issues = mapper.readValue(respuestaJson, new TypeReference<List<IssueDTOGemini>>() {});
            return issues;

        } catch (Exception e) {
            throw new RuntimeException("Error procesando la respuesta de Gemini", e);
        }
    }

    public List<IssueDTOGemini> detectIssuesFromDocx(String projectId, InputStream docxInputStream) {
        try (XWPFDocument document = new XWPFDocument(docxInputStream)) {
            String texto = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));

            return detectIssues(projectId, texto);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo el archivo DOCX", e);
        }
    }

    public String chat(String prompt) {
        GeminiConfig geminiConfig = geminiConfigService.getGeminiConfigForInternalUse();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = geminiConfig.getUrl() + "?key=" + geminiConfig.getKey();

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();

            String rawText = mapper.readTree(response.getBody())
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            return rawText;

        } catch (Exception e) {
            throw new RuntimeException("Error procesando la respuesta de Gemini: " + e.getMessage(), e);
        }
    }

    public String chat(String prompt, MultipartFile[] archivos) {
        StringBuilder contenidoArchivos = new StringBuilder();

        if (archivos != null) {
            for (MultipartFile archivo : archivos) {
                String nombre = archivo.getOriginalFilename();
                contenidoArchivos.append("\n\n--- Contenido de '").append(nombre).append("' ---\n");

                try {
                    contenidoArchivos.append(extractTextFromFile(archivo));
                } catch (Exception e) {
                    contenidoArchivos.append("[Error leyendo archivo ").append(nombre).append("]: ")
                            .append(e.getMessage()).append("\n");
                }
            }
        }

        return chat(prompt + "\n\n" + contenidoArchivos.toString());
    }

    private String extractTextFromFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) throw new IllegalArgumentException("Archivo sin nombre");

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return switch (ext) {
            case "docx" -> extractTextFromDocx(file);
            case "xlsx" -> extractTextFromExcel(file);
            case "pdf" -> extractTextFromPdf(file);
            default -> extractTextAsPlainText(file);
        };
    }

    private String extractTextFromDocx(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));

        } catch (IOException e) {
            throw new RuntimeException("Error procesando archivo Word: " + e.getMessage(), e);
        }
    }

    private String extractTextFromExcel(MultipartFile file) {
        StringBuilder sb = new StringBuilder();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            for (Sheet sheet : workbook) {
                sb.append("Hoja: ").append(sheet.getSheetName()).append("\n");

                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(getCellValueAsString(cell)).append(" | ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }

        } catch (Exception e) {
            throw new RuntimeException("Error procesando archivo Excel: " + e.getMessage(), e);
        }

        return sb.toString();
    }

    private String extractTextAsPlainText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo como texto plano", e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> "";
            default -> "[UNKNOWN]";
        };
    }

    private String extractTextFromPdf(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            PDDocument document = PDDocument.load(is);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        } catch (IOException e) {
            throw new RuntimeException("Error procesando archivo PDF: " + e.getMessage(), e);
        }
    }
}
