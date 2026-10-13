package com.smartprocure.ai.dto;

import java.util.Collections;
import java.util.List;

/**
 * Request payload sent to Google Gemini generateContent endpoint.
 */
public class GeminiRequest {

    private List<GeminiContent> contents;

    public GeminiRequest() {
    }

    public GeminiRequest(String promptText) {
        this.contents = Collections.singletonList(new GeminiContent(promptText));
    }

    public GeminiRequest(List<GeminiContent> contents) {
        this.contents = contents;
    }

    public List<GeminiContent> getContents() {
        return contents;
    }

    public void setContents(List<GeminiContent> contents) {
        this.contents = contents;
    }
}
