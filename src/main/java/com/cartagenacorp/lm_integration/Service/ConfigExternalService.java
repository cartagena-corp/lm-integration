package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.dto.IssueDescriptionsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConfigExternalService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigExternalService.class);

    @Value("${config.service.url}")
    private String configServiceUrl;

    private final RestTemplate restTemplate;

    public ConfigExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<List<IssueDescriptionsDto>> getIssueDescription(String token, UUID projectId) {
        try {
            logger.debug("Solicitando descripciones de issues del proyecto: {}", projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<String>> entity = new HttpEntity<>(headers);

            ResponseEntity<List<IssueDescriptionsDto>> response = restTemplate.exchange(
                    configServiceUrl + "/issue-descriptions/" + projectId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            logger.info("Datos de descripciones de issues obtenidos exitosamente. Cantidad: {}",
                    response.getBody() != null ? response.getBody().size() : 0);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            logger.error("Error al obtener datos de descripciones de las issues del proyecto: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
