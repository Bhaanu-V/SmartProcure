package com.smartprocure.ai.client;

import com.smartprocure.ai.config.GeminiProperties;
import com.smartprocure.ai.dto.GeminiRequest;
import com.smartprocure.ai.dto.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Secure HTTP client for calling Google Gemini Generative AI endpoints.
 * Never logs credentials, enforces connection/read timeouts, and returns Optional for safe degradation.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Sends a prompt to Gemini and returns the textual response, or empty Optional if unavailable or errored.
     */
    public Optional<String> generateText(String prompt) {
        if (!properties.isConfigured()) {
            log.info("Gemini API key is not configured; using deterministic fallback analysis.");
            return Optional.empty();
        }

        try {
            String uri = String.format("%s/%s:generateContent?key=%s",
                    properties.getEndpoint(),
                    properties.getModel(),
                    properties.getApiKey());

            GeminiRequest request = new GeminiRequest(prompt);

            GeminiResponse response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response != null && response.getFirstText() != null) {
                return Optional.of(response.getFirstText().trim());
            }
        } catch (Exception ex) {
            log.warn("Gemini API invocation encountered a recoverable failure: {}. Proceeding with deterministic fallback.", ex.getMessage());
        }

        return Optional.empty();
    }

    public GeminiProperties getProperties() {
        return properties;
    }
}
