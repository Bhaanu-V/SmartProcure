package com.smartprocure.ai.dto;

/**
 * Status response indicating AI service operational state and human-in-the-loop safeguards.
 */
public class AiStatusResponse {

    private boolean configured;
    private String model;
    private String serviceStatus;
    private String humanInTheLoopPolicy;

    public AiStatusResponse() {
    }

    public AiStatusResponse(boolean configured, String model, String serviceStatus, String humanInTheLoopPolicy) {
        this.configured = configured;
        this.model = model;
        this.serviceStatus = serviceStatus;
        this.humanInTheLoopPolicy = humanInTheLoopPolicy;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public String getHumanInTheLoopPolicy() {
        return humanInTheLoopPolicy;
    }

    public void setHumanInTheLoopPolicy(String humanInTheLoopPolicy) {
        this.humanInTheLoopPolicy = humanInTheLoopPolicy;
    }
}
