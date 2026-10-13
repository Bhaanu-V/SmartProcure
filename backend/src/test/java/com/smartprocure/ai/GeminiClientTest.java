package com.smartprocure.ai;

import com.smartprocure.ai.client.GeminiClient;
import com.smartprocure.ai.config.GeminiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeminiClient Unit Tests")
class GeminiClientTest {

    private GeminiProperties properties;
    private GeminiClient geminiClient;

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties();
        geminiClient = new GeminiClient(properties);
    }

    @Test
    @DisplayName("Should detect unavailable when API key is null")
    void testIsAvailableWhenKeyIsNull() {
        properties.setApiKey(null);
        assertFalse(geminiClient.isAvailable());
        assertEquals(Optional.empty(), geminiClient.generateText("Test prompt"));
    }

    @Test
    @DisplayName("Should detect unavailable when API key is placeholder")
    void testIsAvailableWhenKeyIsPlaceholder() {
        properties.setApiKey("test-gemini-key");
        assertFalse(geminiClient.isAvailable());
        assertEquals(Optional.empty(), geminiClient.generateText("Test prompt"));
    }

    @Test
    @DisplayName("Should detect unavailable when API key is blank")
    void testIsAvailableWhenKeyIsBlank() {
        properties.setApiKey("   ");
        assertFalse(geminiClient.isAvailable());
        assertEquals(Optional.empty(), geminiClient.generateText("Test prompt"));
    }

    @Test
    @DisplayName("Should return empty optional for blank prompt even if configured")
    void testGenerateTextWithBlankPrompt() {
        properties.setApiKey("valid-looking-api-key");
        assertEquals(Optional.empty(), geminiClient.generateText("   "));
        assertEquals(Optional.empty(), geminiClient.generateText(null));
    }
}
