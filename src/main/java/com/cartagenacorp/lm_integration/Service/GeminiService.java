package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.GeminiResponseDTO;
import com.cartagenacorp.lm_integration.dto.IssueDescriptionsDto;
import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private final GeminiConfigService geminiConfigService;
    private final ConfigExternalService configExternalService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiService(GeminiConfigService geminiConfigService,
                         ConfigExternalService configExternalService) {
        this.geminiConfigService = geminiConfigService;
        this.configExternalService = configExternalService;
    }

    public GeminiResponseDTO detectIssuesFromTextAndOrDocx(String projectId, String promptTexto, MultipartFile file) {
        StringBuilder contenidoArchivo = new StringBuilder();

        if (file != null && !file.isEmpty()) {
            try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
                String textoDocx = document.getParagraphs().stream()
                        .map(XWPFParagraph::getText)
                        .collect(Collectors.joining("\n"));
                contenidoArchivo.append(textoDocx);
            } catch (IOException e) {
                throw new RuntimeException("Error procesando archivo DOCX", e);
            }
        }

        if ((promptTexto == null || promptTexto.isBlank()) && contenidoArchivo.isEmpty()) {
            throw new IllegalArgumentException("Debes enviar al menos un prompt o un archivo para analizar.");
        }

        if (promptTexto == null || promptTexto.isBlank()) {
            promptTexto = "Extrae tareas del siguiente contenido:";
        }

        UUID projectIdUUID = UUID.fromString(projectId);
        List<String> descriptionTitles = configExternalService.getIssueDescription(JwtContextHolder.getToken(), projectIdUUID)
                .orElse(List.of())
                .stream()
                .map(IssueDescriptionsDto::getName)
                .collect(Collectors.toList());

        return detectIssues(projectId, promptTexto, contenidoArchivo.toString(), descriptionTitles);
    }

    public GeminiResponseDTO detectIssues(String projectId, String promptUsuario, String contenidoArchivo, List<String> descriptionTitles) {
        GeminiConfig geminiConfig = geminiConfigService.getGeminiConfigForInternalUse();

        String titlesText = "";
        if (descriptionTitles != null && !descriptionTitles.isEmpty()) {
            titlesText = "Usa exactamente estos títulos para las descripciones:\n";
            for (String title : descriptionTitles) {
                titlesText += "- " + title + "\n";
            }
            titlesText += "Si no encuentras contenido para alguno, aún así inclúyelo con un campo 'text' vacío.\n";
        }

        String prompt = String.format("""
        Devuelve un JSON como el siguiente. No me devuelvas nada más que el JSON entre ```json y ```:
        Si no hay contenido para analizar devuelve el array de issues vacio, a menos que la instruccion del usuario
        te pida generar tareas o te pase algun texto de donde puedas detectar tareas.
        
        {
          "response": "Respuesta directa a la siguiente instrucción: '%s'",
          "issues": [
            {
              "title": "CREAR CRUD DE USUARIOS",
              "descriptionsDTO": [
                { "title": "Requerimientos", "text": "El sistema debe permitir crear, leer, actualizar y eliminar usuarios" },
                { "title": "Validaciones", "text": "El correo debe ser único" }
              ],
              "projectId": Asegúrate de que cada objeto en la lista tenga en este campo 'projectId' el valor "%s".,
              "assignedId": equipo, persona mencionada, no se deja claro
            }
          ]
        }
        
        %s
        
        Instrucción del usuario (para llenar el campo 'response'): %s
        
        --- CONTENIDO A ANALIZAR ---
        %s
        """, promptUsuario, projectId, titlesText, promptUsuario, contenidoArchivo);

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

            String jsonClean = rawText
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            return mapper.readValue(jsonClean, GeminiResponseDTO.class);

        } catch (Exception e) {
            throw new RuntimeException("Error procesando la respuesta de Gemini", e);
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
