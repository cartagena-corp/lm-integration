package com.cartagenacorp.lm_integration.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gemini.api")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiProperties {
    private String url;
    private String key;
}
