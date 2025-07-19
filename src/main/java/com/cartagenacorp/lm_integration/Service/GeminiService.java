package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.IssueDTOGemini;
import com.cartagenacorp.lm_integration.util.GeminiProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final GeminiProperties properties;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private final RestTemplate restTemplate = new RestTemplate();

    private final IssueExternalService issueExternalService;

    public GeminiService(GeminiProperties properties, IssueExternalService issueExternalService) {
        this.properties = properties;
        this.issueExternalService = issueExternalService;
    }


    public List<IssueDTOGemini> detectIssues(String projectId, String texto) {
        String prompt = String.format("""
        Analiza el siguiente texto y devuelve una lista de tareas en formato JSON como este:
        Me devuelvas ninguna otra respuesta aparte del ```json y el ``` para encerrar el array de issues
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
        //String url = GEMINI_API_URL + apiKey;
        String url = properties.getUrl() + "?key=" + properties.getKey();

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
}
