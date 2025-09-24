package com.cartagenacorp.lm_integration.Service;

import com.cartagenacorp.lm_integration.controller.OrganizationResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
public class OrganizationExternalService {
    private static final Logger logger = LoggerFactory.getLogger(OrganizationExternalService.class);

    @Value("${organization.service.url}")
    private String organizationServiceUrl;

    private final RestTemplate restTemplate;

    public OrganizationExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<String> getOrganizationName(String token, UUID organizationId) {
        try {
            logger.debug("Solicitando nombre de la organización con ID: {}", organizationId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<OrganizationResponseDto> response = restTemplate.exchange(
                    organizationServiceUrl + "/userOrganization/" + organizationId,
                    HttpMethod.GET,
                    entity,
                    OrganizationResponseDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("Nombre de la organización obtenido exitosamente: {}", response.getBody().getOrganizationName());
                return Optional.ofNullable(response.getBody().getOrganizationName());
            }

            logger.warn("No se encontró la organización con ID: {}", organizationId);
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error al obtener el nombre de la organización con ID {}: {}", organizationId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
