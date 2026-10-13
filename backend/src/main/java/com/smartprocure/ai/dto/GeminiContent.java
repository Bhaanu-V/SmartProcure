package com.smartprocure.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;

/**
 * Gemini API content container.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiContent {

    private List<GeminiPart> parts;

    public GeminiContent() {
    }

    public GeminiContent(String text) {
        this.parts = Collections.singletonList(new GeminiPart(text));
    }

    public GeminiContent(List<GeminiPart> parts) {
        this.parts = parts;
    }

    public List<GeminiPart> getParts() {
        return parts;
    }

    public void setParts(List<GeminiPart> parts) {
        this.parts = parts;
    }
}
