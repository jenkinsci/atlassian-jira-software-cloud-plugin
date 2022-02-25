package com.atlassian.jira.cloud.jenkins.common.client;

import com.fasterxml.jackson.annotation.JsonValue;

import java.time.LocalDateTime;

public class JenkinsAppRequest {

    private final RequestType requestType;
    private final EventType eventType;
    private final String pipelineName;
    private final String status;
    private final String lastUpdated;
    private final JiraRequest payload;
    private final String pipelineId;

    public JenkinsAppRequest(
            final RequestType requestType,
            final EventType eventType,
            final String pipelineId,
            final String pipelineName,
            final String status,
            final String lastUpdated,
            final JiraRequest payload) {
        this.requestType = requestType;
        this.eventType = eventType;
        this.pipelineId = pipelineId;
        this.pipelineName = pipelineName;
        this.status = status;
        this.lastUpdated = lastUpdated;
        this.payload = payload;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getStatus() {
        return status;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public JiraRequest getPayload() {
        return payload;
    }

    public enum RequestType {
        EVENT("event");

        private final String value;

        RequestType(final String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public enum EventType {
        BUILD("build"),
        DEPLOYMENT("deployment");

        private final String value;

        EventType(final String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }
}
