package com.smartprocure.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Gemini API text part structure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiPart {

    private String text;

    public GeminiPart() {
    }

    public GeminiPart(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
