package com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GatingStatus {
    ALLOWED("allowed"),
    PREVENTED("prevented"),
    AWAITING("awaiting"),
    EXPIRED("expired"),
    INVALID("invalid");

    public final String status;

    GatingStatus(final String status) {
        this.status = status;
    }

    @JsonValue
    public String getStatus() {
        return status;
    }
}
