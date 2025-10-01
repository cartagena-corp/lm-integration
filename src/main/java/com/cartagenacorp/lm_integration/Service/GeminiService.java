package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.GeminiResponseDTO;
import com.cartagenacorp.lm_integration.dto.IssueDescriptionsDto;
import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import com.cartagenacorp.lm_integration.util.ApiUsageLogger;
import com.cartagenacorp.lm_integration.util.JwtContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final GeminiConfigService geminiConfigService;
    private final ConfigExternalService configExternalService;
    private final ApiUsageLogger apiUsageLogger;

    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiService(GeminiConfigService geminiConfigService,
                         ConfigExternalService configExternalService,
                         ApiUsageLogger apiUsageLogger) {
        this.geminiConfigService = geminiConfigService;
        this.configExternalService = configExternalService;
        this.apiUsageLogger = apiUsageLogger;
    }

    public GeminiResponseDTO detectIssuesFromTextAndOrDocx(String projectId, String promptTexto, MultipartFile file) {
        long startTime = System.currentTimeMillis();;
        String status = "OK";
        String feature = "detect-issues";

        logger.info("=== [GeminiService] Iniciando flujo de detección de Issues de texto/documento para projectId={} ===", projectId);

        try {
            StringBuilder contenidoArchivo = new StringBuilder();

            if (file != null && !file.isEmpty()) {
                logger.info("[GeminiService] Procesando archivo DOCX: {}", file.getOriginalFilename());
                try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
                    String textoDocx = document.getParagraphs().stream()
                            .map(XWPFParagraph::getText)
                            .collect(Collectors.joining("\n"));
                    contenidoArchivo.append(textoDocx);
                } catch (IOException e) {
                    logger.error("[GeminiService] Error procesando archivo DOCX", e);
                    throw new RuntimeException("Error procesando archivo DOCX", e);
                }
            }

            if ((promptTexto == null || promptTexto.isBlank()) && contenidoArchivo.isEmpty()) {
                logger.warn("[GeminiService] No se recibió ni prompt ni archivo para analizar");
                throw new IllegalArgumentException("Debes enviar al menos un prompt o un archivo para analizar.");
            }

            if (promptTexto == null || promptTexto.isBlank()) {
                logger.debug("[GeminiService] Prompt vacío, asignando prompt por defecto");
                promptTexto = "Extrae tareas del siguiente contenido:";
            }

            UUID projectIdUUID = UUID.fromString(projectId);
            List<String> descriptionTitles = configExternalService.getIssueDescription(JwtContextHolder.getToken(), projectIdUUID)
                    .orElse(List.of())
                    .stream()
                    .map(IssueDescriptionsDto::getName)
                    .collect(Collectors.toList());

            return detectIssues(projectId, promptTexto, contenidoArchivo.toString(), descriptionTitles);
        } catch (Exception e) {
            status = "ERROR";
            logger.error("[GeminiService] Error en detectIssuesFromTextAndOrDocx para projectId={}", projectId, e);
            throw e;
        } finally {
            apiUsageLogger.log(feature, projectId, startTime, status);
            logger.info("[GeminiService] Finalizando flujo de detección de Issues de texto/documento para projectId={} con status={}", projectId, status);
        }
    }

    public GeminiResponseDTO detectIssues(String projectId, String promptUsuario, String contenidoArchivo, List<String> descriptionTitles) {
        logger.info("[GeminiService] Iniciando flujo de consulta a Gemini para projectId={}", projectId);

        GeminiConfig geminiConfig = geminiConfigService.getGeminiConfigForInternalUse();

        String titlesText = "";
        if (descriptionTitles != null && !descriptionTitles.isEmpty()) {
            titlesText = "Usa exactamente estos títulos para las descripciones:\n";
            for (String title : descriptionTitles) {
                titlesText += "- " + title + "\n";
            }
            titlesText += "y del contexto, identifica el contenido y relacionalo para cada una. Si no hay nada relacionado a esa descripción, inclúye el campo 'text' vacío.\n";
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
        logger.info("[GeminiService] Enviando request a Gemini API: {}", url);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        logger.debug("[GeminiService] Respuesta recibida: {}", response.getBody());

        try {
            ObjectMapper mapper = new ObjectMapper();

            String rawText = mapper.readTree(response.getBody())
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            logger.debug("[GeminiService] Texto extraído: {}", rawText);

            String jsonClean = rawText
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            GeminiResponseDTO dto = mapper.readValue(jsonClean, GeminiResponseDTO.class);
            logger.info("[GeminiService] Finalizando flujo de consulta a Gemini para projectId={}", projectId);
            return dto;
        } catch (Exception e) {
            logger.error("[GeminiService] Error procesando respuesta de Gemini para projectId={}", projectId, e);
            throw new RuntimeException("Error procesando la respuesta de Gemini", e);
        }
    }

    public String chat(String prompt) {
        logger.info("[GeminiService] chat iniciado");
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
        logger.info("[GeminiService] Enviando request a Gemini API: {}", url);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        logger.debug("[GeminiService] Respuesta recibida: {}", response.getBody());

        try {
            ObjectMapper mapper = new ObjectMapper();

            String rawText = mapper.readTree(response.getBody())
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            logger.info("[GeminiService] chat completado con éxito");
            return rawText;

        } catch (Exception e) {
            logger.error("[GeminiService] Error procesando respuesta de Gemini en chat", e);
            throw new RuntimeException("Error procesando la respuesta de Gemini: " + e.getMessage(), e);
        }
    }

    public String chat(String prompt, MultipartFile[] archivos) {
        long startTime = System.currentTimeMillis();
        String status = "OK";
        String feature = "chat";

        logger.info("[GeminiService] chat (con archivos) iniciado");

        try {
            StringBuilder contenidoArchivos = new StringBuilder();

            if (archivos != null) {
                for (MultipartFile archivo : archivos) {
                    String nombre = archivo.getOriginalFilename();
                    logger.info("[GeminiService] Procesando archivo: {}", nombre);
                    contenidoArchivos.append("\n\n--- Contenido de '").append(nombre).append("' ---\n");

                    try {
                        contenidoArchivos.append(extractTextFromFile(archivo));
                    } catch (Exception e) {
                        logger.error("[GeminiService] Error leyendo archivo {}", nombre, e);
                        contenidoArchivos.append("[Error leyendo archivo ").append(nombre).append("]: ").append(e.getMessage()).append("\n");
                    }
                }
            }

            return chat(prompt + "\n\n" + contenidoArchivos.toString());
        } catch (Exception e) {
            status = "ERROR";
            logger.error("[GeminiService] Error en chat con archivos", e);
            throw e;
        } finally {
            apiUsageLogger.log(feature, null, startTime, status);
            logger.info("[GeminiService] chat (con archivos) finalizado con status={}", status);
        }
    }

    private String extractTextFromFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            logger.warn("[GeminiService] Archivo sin nombre recibido");
            throw new IllegalArgumentException("Archivo sin nombre");
        }

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        logger.info("[GeminiService] Extrayendo texto de archivo {} con extensión {}", filename, ext);

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

            logger.debug("[GeminiService] Extrayendo texto desde DOCX: {}", file.getOriginalFilename());
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));

        } catch (IOException e) {
            logger.error("[GeminiService] Error procesando archivo Word {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Error procesando archivo Word: " + e.getMessage(), e);
        }
    }

    private String extractTextFromExcel(MultipartFile file) {
        StringBuilder sb = new StringBuilder();
        logger.debug("[GeminiService] Extrayendo texto desde Excel: {}", file.getOriginalFilename());

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
            logger.error("[GeminiService] Error procesando archivo Excel {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Error procesando archivo Excel: " + e.getMessage(), e);
        }

        return sb.toString();
    }

    private String extractTextAsPlainText(MultipartFile file) {
        try {
            logger.debug("[GeminiService] Extrayendo texto como plano de: {}", file.getOriginalFilename());
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("[GeminiService] Error leyendo archivo como texto plano {}", file.getOriginalFilename(), e);
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
            logger.debug("[GeminiService] Extrayendo texto desde PDF: {}", file.getOriginalFilename());
            PDDocument document = PDDocument.load(is);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        } catch (IOException e) {
            logger.error("[GeminiService] Error procesando archivo PDF {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Error procesando archivo PDF: " + e.getMessage(), e);
        }
    }
}
