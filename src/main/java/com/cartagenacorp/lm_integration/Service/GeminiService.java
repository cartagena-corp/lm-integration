package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.IssueDTOGemini;
import com.cartagenacorp.lm_integration.entity.GeminiConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
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
}
